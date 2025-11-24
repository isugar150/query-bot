package com.namejm.query_bot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.namejm.query_bot.config.AppProperties;
import com.namejm.query_bot.domain.ChatMessage;
import com.namejm.query_bot.domain.ChatSession;
import com.namejm.query_bot.domain.DatabaseConnection;
import com.namejm.query_bot.dto.ChatMessageDto;
import com.namejm.query_bot.dto.ChatRequest;
import com.namejm.query_bot.dto.ChatResponse;
import com.namejm.query_bot.dto.ChatSessionSummary;
import com.namejm.query_bot.dto.SchemaOverview;
import com.namejm.query_bot.model.MessageRole;
import com.namejm.query_bot.repository.ChatMessageRepository;
import com.namejm.query_bot.repository.ChatSessionRepository;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

@Service
public class ChatService {
    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final DatabaseService databaseService;
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ObjectMapper objectMapper;
    private final AppProperties appProperties;
    private final RestClient restClient;

    public ChatService(DatabaseService databaseService, ChatSessionRepository chatSessionRepository, ChatMessageRepository chatMessageRepository,
                       ObjectMapper objectMapper, AppProperties appProperties) {
        this.databaseService = databaseService;
        this.chatSessionRepository = chatSessionRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.objectMapper = objectMapper;
        this.appProperties = appProperties;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public ChatResponse ask(ChatRequest request) throws Exception {
        DatabaseConnection database = databaseService.findById(request.dbId())
                .orElseThrow(() -> new IllegalArgumentException("데이터베이스 정보를 찾을 수 없습니다."));
        if (!database.isSchemaReady()) {
            throw new IllegalStateException("해당 데이터베이스의 스키마를 아직 수집 중입니다. 잠시 후 다시 시도해주세요.");
        }

        ChatSession session = resolveSession(request, database);

        List<ChatMessage> priorHistory = chatMessageRepository.findBySessionOrderByCreatedAtAsc(session);

        ChatMessage userMessage = new ChatMessage();
        userMessage.setSession(session);
        userMessage.setRole(MessageRole.USER);
        userMessage.setContent(request.message());

        // Always rebuild the system prompt from the selected database to avoid stale schemas leaking across DBs.
        SchemaOverview schema = databaseService.fetchLiveSchema(database);
        String systemPrompt = buildSystemPrompt(schema);
        session.setSystemPrompt(systemPrompt);
        session.setSystemPromptDatabaseId(database.getId());
        chatSessionRepository.save(session);

        List<ChatMessage> promptHistory = new ArrayList<>(priorHistory);
        promptHistory.add(userMessage);
        String reply = generateAnswer(promptHistory, systemPrompt);

        ChatMessage assistantMessage = new ChatMessage();
        assistantMessage.setSession(session);
        assistantMessage.setRole(MessageRole.ASSISTANT);
        assistantMessage.setContent(reply);

        chatMessageRepository.save(userMessage);
        chatMessageRepository.save(assistantMessage);

        List<ChatMessageDto> historyDto = chatMessageRepository.findBySessionOrderByCreatedAtAsc(session).stream()
                .map(msg -> new ChatMessageDto(msg.getRole(), msg.getContent(), msg.getCreatedAt()))
                .toList();

        return new ChatResponse(session.getId(), reply, historyDto, session.getMetabaseCardId());
    }

    public Optional<ChatResponse> history(Long sessionId) {
        return chatSessionRepository.findById(sessionId)
                .map(session -> {
                    List<ChatMessageDto> history = historyForSession(session);
                    return new ChatResponse(sessionId, "", history, session.getMetabaseCardId());
                });
    }

    public List<ChatMessageDto> historyForSession(ChatSession session) {
        return chatMessageRepository.findBySessionOrderByCreatedAtAsc(session).stream()
                .map(msg -> new ChatMessageDto(msg.getRole(), msg.getContent(), msg.getCreatedAt()))
                .toList();
    }

    public Optional<ChatResponse> latestForDatabase(Long dbId) {
        return databaseService.findById(dbId)
                .flatMap(chatSessionRepository::findFirstByDatabaseConnectionOrderByCreatedAtDesc)
                .map(session -> new ChatResponse(
                        session.getId(),
                        "",
                        historyForSession(session),
                        session.getMetabaseCardId()
                ));
    }

    public List<ChatSessionSummary> sessions(Long dbId) {
        DatabaseConnection db = databaseService.findById(dbId)
                .orElseThrow(() -> new IllegalArgumentException("데이터베이스 정보를 찾을 수 없습니다."));
        return chatSessionRepository.findByDatabaseConnectionOrderByCreatedAtDesc(db).stream()
                .map(session -> new ChatSessionSummary(session.getId(), db.getId(), session.getTitle(), session.getCreatedAt(), session.getMetabaseCardId()))
                .toList();
    }

    public ChatSessionSummary createSession(Long dbId, String title) {
        DatabaseConnection db = databaseService.findById(dbId)
                .orElseThrow(() -> new IllegalArgumentException("데이터베이스 정보를 찾을 수 없습니다."));
        ChatSession created = createSession(db, title);
        return new ChatSessionSummary(created.getId(), dbId, created.getTitle(), created.getCreatedAt(), created.getMetabaseCardId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteSession(Long sessionId) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("세션을 찾을 수 없습니다."));
        // Remove messages first to avoid orphan records.
        chatMessageRepository.deleteAllBySession(session);
        chatSessionRepository.delete(session);
    }

    private ChatSession resolveSession(ChatRequest request, DatabaseConnection database) {
        if (request.sessionId() != null) {
            Optional<ChatSession> existing = chatSessionRepository.findById(request.sessionId());
            if (existing.isPresent()) {
                ChatSession session = existing.get();
                if (!session.getDatabaseConnection().getId().equals(database.getId())) {
                    throw new IllegalArgumentException("선택한 세션이 현재 데이터베이스와 일치하지 않습니다.");
                }
                return session;
            }
        }
        ChatSession session = new ChatSession();
        session.setDatabaseConnection(database);
        String title = request.sessionTitle() != null && !request.sessionTitle().isBlank()
                ? safeTitle(request.sessionTitle(), database.getName())
                : buildTitle(request.message(), database.getName());
        session.setTitle(title);
        return chatSessionRepository.save(session);
    }

    public ChatSession createSession(DatabaseConnection database, String title) {
        ChatSession session = new ChatSession();
        session.setDatabaseConnection(database);
        session.setTitle(safeTitle(title, database.getName()));
        return chatSessionRepository.save(session);
    }

    private String buildTitle(String message, String dbName) {
        String clean = message.length() > 40 ? message.substring(0, 40) + "..." : message;
        return "[" + dbName + "] " + clean;
    }

    private String safeTitle(String raw, String dbName) {
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return buildTitle("새 세션", dbName);
        }
        return trimmed.length() > 60 ? trimmed.substring(0, 60) + "..." : trimmed;
    }

    private SchemaOverview loadSchema(DatabaseConnection databaseConnection) throws Exception {
        if (databaseConnection.getSchemaJson() == null) {
            throw new IllegalStateException("해당 데이터베이스의 스키마 정보가 없습니다. 다시 수집하세요.");
        }
        return objectMapper.readValue(databaseConnection.getSchemaJson(), SchemaOverview.class);
    }

    private String generateAnswer(List<ChatMessage> history, String systemPrompt) throws Exception {
        if (appProperties.getOpenai().getApiKey() == null || appProperties.getOpenai().getApiKey().isBlank()) {
            return "OPENAI_API_KEY가 설정되지 않아 예시 답변을 반환합니다.\n--\nSELECT * FROM sample_table WHERE condition;";
        }

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of(
                "role", "system",
                "content", systemPrompt
        ));
        for (ChatMessage msg : history) {
            messages.add(Map.of(
                    "role", msg.getRole() == MessageRole.USER ? "user" : "assistant",
                    "content", msg.getContent()
            ));
        }

        OpenAiResponse response = restClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .headers(headers -> headers.setBearerAuth(appProperties.getOpenai().getApiKey()))
                .body(Map.of(
                        "model", appProperties.getOpenai().getModel(),
                        "messages", messages
                ))
                .retrieve()
                .body(OpenAiResponse.class);

        if (response == null || response.choices().isEmpty()) {
            throw new IllegalStateException("AI 응답을 받지 못했습니다.");
        }
        return response.choices().get(0).message().content();
    }

    private String buildSystemPrompt(SchemaOverview schema) {
        StringBuilder builder = new StringBuilder();
        builder.append("You are a SQL expert for the following database. ")
                .append("Use ONLY the provided schema. If the question is ambiguous, ask for clarification in Korean. ")
                .append("When the query is unambiguous and valid, respond with the SQL only. ")
                .append("Always include the schema prefix (schema.table) for every table reference. ")
                .append("Return exactly one SELECT statement (no multiple statements like `SELECT 1; SELECT 2;`). ")
                .append("Do NOT add LIMIT unless the user explicitly asks for a row limit or pagination. ")
                .append("Pay close attention to table/column comments; prefer tables whose comments match the request and ignore unrelated tables even if they look similar. ")
                .append("Never invent tables or columns; before answering, verify every table/column you reference exists in the schema below. ")
                .append("Never create synonym columns (e.g., order_datetime when only register_ymdt exists); use exact column names from the schema or ask the user to choose one. ")
                .append("If the user describes a concept (e.g., 주문 시각), map it to an existing column ONLY if the name or comment clearly matches; otherwise ask for clarification instead of guessing. ")
                .append("Do NOT infer missing start/end date columns (e.g., start_datetime/end_datetime) from comments or intent—only use columns that exist exactly as listed. ")
                .append("If the user asks for a period or date range but no obvious start/end columns exist, ask the user to pick specific columns instead of guessing. ")
                .append("Treat comments as hints only; never treat them as alternative column names. ")
                .append("If you cannot find an exact table or column match, DO NOT guess—reply in Korean that the table/column does not exist and list the available options instead of generating SQL. ")
                .append("Before returning SQL, run this checklist: (1) list the tables you will use; (2) for each table, ensure every column you use appears in that table's column list; (3) if any column is missing, do not return SQL—respond in Korean that it is missing and show only the columns that exist.\n\n");
        builder.append("Database: ").append(schema.database());
        if (schema.schemas() != null && !schema.schemas().isEmpty()) {
            builder.append(" (schemas: ").append(String.join(", ", schema.schemas())).append(")");
        }
        builder.append("\n");
        for (var table : schema.tables()) {
            builder.append("- ").append(table.schema()).append(".").append(table.name());
            if (table.comment() != null && !table.comment().isBlank()) {
                builder.append(" -- ").append(table.comment());
            }
            builder.append(" (");
            List<String> columns = table.columns().stream()
                    .map(col -> {
                        String base = col.name() + " " + col.type() + (col.nullable() ? "" : " NOT NULL");
                        if (col.comment() != null && !col.comment().isBlank()) {
                            return base + " -- " + col.comment();
                        }
                        return base;
                    })
                    .toList();
            builder.append(String.join(", ", columns)).append(")\n");
        }
        builder.append("\nReturn only SQL when possible.");
        return builder.toString();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OpenAiResponse(@JsonProperty("choices") List<Choice> choices) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Choice(@JsonProperty("message") Message message) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Message(String content, String role) {
    }
}
