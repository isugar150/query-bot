package com.namejm.query_bot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.namejm.query_bot.config.AppProperties;
import com.namejm.query_bot.domain.ChatMessage;
import com.namejm.query_bot.domain.ChatSession;
import com.namejm.query_bot.domain.DatabaseConnection;
import com.namejm.query_bot.dto.ChatMessageDto;
import com.namejm.query_bot.dto.ChatRequest;
import com.namejm.query_bot.dto.ChatResponse;
import com.namejm.query_bot.dto.SchemaOverview;
import com.namejm.query_bot.model.MessageRole;
import com.namejm.query_bot.repository.ChatMessageRepository;
import com.namejm.query_bot.repository.ChatSessionRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
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

    public ChatResponse ask(ChatRequest request) throws Exception {
        DatabaseConnection database = databaseService.findById(request.dbId())
                .orElseThrow(() -> new IllegalArgumentException("데이터베이스 정보를 찾을 수 없습니다."));
        if (!database.isSchemaReady()) {
            throw new IllegalStateException("해당 데이터베이스의 스키마를 아직 수집 중입니다. 잠시 후 다시 시도해주세요.");
        }

        ChatSession session = resolveSession(request, database);
        SchemaOverview schema = loadSchema(database);

        ChatMessage userMessage = new ChatMessage();
        userMessage.setSession(session);
        userMessage.setRole(MessageRole.USER);
        userMessage.setContent(request.message());
        chatMessageRepository.save(userMessage);

        List<ChatMessage> history = chatMessageRepository.findBySessionOrderByCreatedAtAsc(session);
        String reply = generateAnswer(history, schema);

        ChatMessage assistantMessage = new ChatMessage();
        assistantMessage.setSession(session);
        assistantMessage.setRole(MessageRole.ASSISTANT);
        assistantMessage.setContent(reply);
        chatMessageRepository.save(assistantMessage);

        List<ChatMessageDto> historyDto = chatMessageRepository.findBySessionOrderByCreatedAtAsc(session).stream()
                .map(msg -> new ChatMessageDto(msg.getRole(), msg.getContent(), msg.getCreatedAt()))
                .toList();

        return new ChatResponse(session.getId(), reply, historyDto);
    }

    public Optional<ChatResponse> history(Long sessionId) {
        return chatSessionRepository.findById(sessionId)
                .map(session -> {
                    List<ChatMessageDto> history = historyForSession(session);
                    return new ChatResponse(sessionId, "", history);
                });
    }

    public List<ChatMessageDto> historyForSession(ChatSession session) {
        return chatMessageRepository.findBySessionOrderByCreatedAtAsc(session).stream()
                .map(msg -> new ChatMessageDto(msg.getRole(), msg.getContent(), msg.getCreatedAt()))
                .toList();
    }

    private ChatSession resolveSession(ChatRequest request, DatabaseConnection database) {
        if (request.sessionId() != null) {
            Optional<ChatSession> existing = chatSessionRepository.findById(request.sessionId());
            if (existing.isPresent()) {
                return existing.get();
            }
        }
        ChatSession session = new ChatSession();
        session.setDatabaseConnection(database);
        session.setTitle(buildTitle(request.message(), database.getName()));
        return chatSessionRepository.save(session);
    }

    private String buildTitle(String message, String dbName) {
        String clean = message.length() > 40 ? message.substring(0, 40) + "..." : message;
        return "[" + dbName + "] " + clean;
    }

    private SchemaOverview loadSchema(DatabaseConnection databaseConnection) throws Exception {
        if (databaseConnection.getSchemaJson() == null) {
            throw new IllegalStateException("해당 데이터베이스의 스키마 정보가 없습니다. 다시 수집하세요.");
        }
        return objectMapper.readValue(databaseConnection.getSchemaJson(), SchemaOverview.class);
    }

    private String generateAnswer(List<ChatMessage> history, SchemaOverview schema) throws Exception {
        if (appProperties.getOpenai().getApiKey() == null || appProperties.getOpenai().getApiKey().isBlank()) {
            return "OPENAI_API_KEY가 설정되지 않아 예시 답변을 반환합니다.\n--\nSELECT * FROM sample_table WHERE condition;";
        }

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of(
                "role", "system",
                "content", systemPrompt(schema)
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
                        "temperature", 0.2,
                        "messages", messages
                ))
                .retrieve()
                .body(OpenAiResponse.class);

        if (response == null || response.choices().isEmpty()) {
            throw new IllegalStateException("AI 응답을 받지 못했습니다.");
        }
        return response.choices().get(0).message().content();
    }

    private String systemPrompt(SchemaOverview schema) {
        StringBuilder builder = new StringBuilder();
        builder.append("You are a SQL expert for the following database. ")
                .append("Use ONLY the provided schema. If the question is ambiguous, ask for clarification in Korean. ")
                .append("When the query is unambiguous and valid, respond with the SQL only.\n\n");
        builder.append("Database: ").append(schema.database()).append("\n");
        for (var table : schema.tables()) {
            builder.append("- ").append(table.name()).append(" (");
            List<String> columns = table.columns().stream()
                    .map(col -> col.name() + " " + col.type())
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
