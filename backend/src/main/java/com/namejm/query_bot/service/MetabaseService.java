package com.namejm.query_bot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.namejm.query_bot.config.AppProperties;
import com.namejm.query_bot.domain.ChatSession;
import com.namejm.query_bot.dto.MetabaseQuestionRequest;
import com.namejm.query_bot.dto.MetabaseQuestionResponse;
import com.namejm.query_bot.repository.ChatSessionRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class MetabaseService {

    private static final Logger log = LoggerFactory.getLogger(MetabaseService.class);
    private static final DateTimeFormatter TITLE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final AppProperties appProperties;
    private final ChatSessionRepository chatSessionRepository;
    private final ObjectMapper objectMapper;

    public MetabaseService(AppProperties appProperties, ChatSessionRepository chatSessionRepository, ObjectMapper objectMapper) {
        this.appProperties = appProperties;
        this.chatSessionRepository = chatSessionRepository;
        this.objectMapper = objectMapper;
    }

    public boolean isAvailable() {
        return isConfigured();
    }

    public MetabaseQuestionResponse createOrUpdateCard(MetabaseQuestionRequest request) {
        if (!isConfigured()) {
            throw new IllegalStateException("Metabase 연동 정보가 설정되지 않았습니다.");
        }

        ChatSession session = chatSessionRepository.findById(request.sessionId())
                .orElseThrow(() -> new IllegalArgumentException("세션을 찾을 수 없습니다."));

        boolean isUpdate = session.getMetabaseCardId() != null && cardExists(session.getMetabaseCardId());

        Map<String, Object> payload = new HashMap<>();
        String resolvedTitle = isUpdate
                ? resolveUpdateTitle(request.title(), session)
                : resolveTitle(request.title(), session.getTitle());
        payload.put("name", resolvedTitle);
        payload.put("description", request.description());
        payload.put("collection_id", appProperties.getMetabase().getCollectionKey());
        payload.put("dataset_query", Map.of(
                "type", "native",
                "native", Map.of("query", request.query()),
                "database", appProperties.getMetabase().getDatabaseKey()
        ));
        payload.put("visualization_settings", Map.of());
        payload.put("display", "table");
        payload.put("cache_ttl", null);
        payload.put("parameters", new Object[0]);
        payload.put("parameter_mappings", new Object[0]);

        String path = isUpdate ? "/api/card/" + session.getMetabaseCardId() : "/api/card";

        log.info("Calling Metabase {}...", isUpdate ? "update" : "create");

        ResponseEntity<String> responseEntity = sendMetabaseRequest(path, isUpdate, payload);

        if (!responseEntity.getStatusCode().is2xxSuccessful()) {
            log.error("Metabase API error status={} body={}", responseEntity.getStatusCode(), responseEntity.getBody());
            throw new IllegalStateException("Metabase API 호출에 실패했습니다: " + responseEntity.getStatusCode());
        }

        MetabaseCardResponse response;
        try {
            String body = responseEntity.getBody();
            response = body != null ? objectMapper.readValue(body, MetabaseCardResponse.class) : null;
        } catch (Exception e) {
            log.error("Metabase 응답 파싱 실패 body={}", responseEntity.getBody(), e);
            throw new IllegalStateException("Metabase 응답을 파싱하지 못했습니다.", e);
        }

        if (response == null || response.id() == null) {
            throw new IllegalStateException("Metabase 카드 생성에 실패했습니다.");
        }
        session.setMetabaseCardId(response.id());
        chatSessionRepository.save(session);
        String url = buildCardUrl(response.id());
        return new MetabaseQuestionResponse(response.id(), response.name(), url);
    }

    private boolean isConfigured() {
        AppProperties.Metabase meta = appProperties.getMetabase();
        return meta.getUrl() != null && !meta.getUrl().isBlank()
                && meta.getApiKey() != null && !meta.getApiKey().isBlank()
                && meta.getDatabaseKey() != null
                && meta.getCollectionKey() != null;
    }

    private RestClient client() {
        return RestClient.builder()
                .baseUrl(trimTrailingSlash(appProperties.getMetabase().getUrl()))
                .build();
    }

    private String resolveTitle(String title, String sessionTitle) {
        if (title != null && !title.isBlank()) {
            return title.trim();
        }
        if (sessionTitle != null && !sessionTitle.isBlank()) {
            return sessionTitle.trim();
        }
        return "새로운 쿼리";
    }

    private String resolveUpdateTitle(String requestedTitle, ChatSession session) {
        if (requestedTitle != null && !requestedTitle.isBlank()) {
            return requestedTitle.trim();
        }
        Long cardId = session.getMetabaseCardId();
        if (cardId != null) {
            String current = fetchExistingCardTitle(cardId);
            if (current != null && !current.isBlank()) {
                return current;
            }
        }
        if (session.getTitle() != null && !session.getTitle().isBlank()) {
            return session.getTitle().trim();
        }
        return "새로운 쿼리";
    }

    public boolean cardExists(Long cardId) {
        if (cardId == null) {
            return false;
        }
        if (!isConfigured()) {
            return true;
        }
        try {
            ResponseEntity<String> response = client().get()
                    .uri("/api/card/" + cardId)
                    .headers(headers -> headers.set("x-api-key", appProperties.getMetabase().getApiKey()))
                    .retrieve()
                    .toEntity(String.class);
            if (response.getStatusCode().value() == 404) {
                return false;
            }
            if (response.getStatusCode().is2xxSuccessful()) {
                return true;
            }
            log.warn("Metabase card exists check non-2xx status={} body={}", response.getStatusCode(), response.getBody());
            return true; // avoid dropping id on transient errors
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound nf) {
            return false;
        } catch (Exception e) {
            log.warn("Metabase card exists check failed for id={}", cardId, e);
            return true; // avoid dropping reference on transient failures
        }
    }

    private String fetchExistingCardTitle(Long cardId) {
        try {
            ResponseEntity<String> response = client().get()
                    .uri("/api/card/" + cardId)
                    .headers(headers -> headers.set("x-api-key", appProperties.getMetabase().getApiKey()))
                    .retrieve()
                    .toEntity(String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                log.warn("Metabase card fetch failed status={}", response.getStatusCode());
                return null;
            }
            String body = response.getBody();
            if (body == null) return null;
            Map<?, ?> parsed = objectMapper.readValue(body, Map.class);
            Object name = parsed.get("name");
            return name != null ? name.toString() : null;
        } catch (Exception e) {
            log.warn("Failed to fetch existing Metabase card title for id={}", cardId, e);
            return null;
        }
    }

    private String trimTrailingSlash(String url) {
        if (url == null) {
            return "";
        }
        String trimmed = url.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    public String buildCardUrl(Long cardId) {
        if (cardId == null) {
            return null;
        }
        String base = trimTrailingSlash(appProperties.getMetabase().getUrl());
        if (base.isBlank()) {
            return null;
        }
        return base + "/question/" + cardId;
    }

    private ResponseEntity<String> sendMetabaseRequest(String path, boolean isUpdate, Map<String, Object> payload) {
        logSend(path, payload);

        return (isUpdate ? client().put().uri(path) : client().post().uri(path))
                .headers(headers -> {
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
                    headers.set("x-api-key", appProperties.getMetabase().getApiKey());
                })
                .body(payload)
                .retrieve()
                .toEntity(String.class);
    }

    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() < 8) return "****";
        return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
    }

    private void logSend(String path, Map<String, Object> payload) {
        try {
            String bodyJson = objectMapper.writeValueAsString(payload);
            log.info(
                    "Metabase request path={} apiKey={} payload={}",
                    path,
                    maskApiKey(appProperties.getMetabase().getApiKey()),
                    bodyJson
            );
        } catch (Exception e) {
            log.warn("Metabase payload logging failed", e);
        }
    }

    private record MetabaseCardResponse(Long id, String name) {
    }
}
