package com.highlands.order.infrastructure.approval;

import com.highlands.order.application.approval.ApprovalPort;
import com.highlands.order.config.DxosApprovalProperties;
import com.highlands.order.exception.ApiDomainException;
import com.highlands.order.exception.ApiErrorCode;
import com.highlands.order.exception.ResourceNotFoundException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Component
@ConditionalOnProperty(prefix = "app.approval", name = "provider", havingValue = "dxos")
public class DxosApiClient {

    private static final ParameterizedTypeReference<ApiEnvelope<Map<String, Object>>> MAP_ENVELOPE =
            new ParameterizedTypeReference<>() { };
    private static final ParameterizedTypeReference<ApiEnvelope<List<Map<String, Object>>>> LIST_ENVELOPE =
            new ParameterizedTypeReference<>() { };
    private static final ParameterizedTypeReference<ApiEnvelope<EntityMetadata>> METADATA_ENVELOPE =
            new ParameterizedTypeReference<>() { };
    private static final ParameterizedTypeReference<Map<String, Object>> MAP_BODY =
            new ParameterizedTypeReference<>() { };

    private final DxosApprovalProperties properties;
    private final RestClient http;
    private final Map<RemoteIdentity, CachedToken> tokens = new ConcurrentHashMap<>();

    public DxosApiClient(DxosApprovalProperties properties) {
        properties.requireConfigured();
        this.properties = properties;
        this.http = client(properties);
    }

    public RecordData create(ApprovalPort.Submission submission) {
        Map<String, Object> body = Map.of(
                "order_id", submission.orderId(),
                "order_code", submission.orderCode(),
                "requester_name", submission.customerName(),
                "total_amount", submission.totalAmount()
        );
        ApiEnvelope<Map<String, Object>> response = apiCall(() -> http.post()
                .uri(dataPath())
                .headers(headers -> headers.setBearerAuth(token(RemoteIdentity.REQUESTER)))
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(MAP_ENVELOPE));
        return record(unwrap(response));
    }

    public RecordData get(String recordId, RemoteIdentity identity) {
        ApiEnvelope<Map<String, Object>> response = apiCall(() -> http.get()
                .uri(dataPath() + "/{id}", recordId)
                .headers(headers -> headers.setBearerAuth(token(identity)))
                .retrieve()
                .body(MAP_ENVELOPE));
        return record(unwrap(response));
    }

    public RecordData act(String recordId, String action, String note, RemoteIdentity identity) {
        Map<String, String> body = note == null || note.isBlank() ? Map.of() : Map.of("note", note.trim());
        ApiEnvelope<Map<String, Object>> response = apiCall(() -> http.post()
                .uri(dataPath() + "/{id}/actions/{action}", recordId, action)
                .headers(headers -> headers.setBearerAuth(token(identity)))
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(MAP_ENVELOPE));
        return record(unwrap(response));
    }

    public EntityMetadata metadata(RemoteIdentity identity) {
        ApiEnvelope<EntityMetadata> response = apiCall(() -> http.get()
                .uri("/api/v1/meta/models/{model}/entities/{entity}",
                        properties.model(), properties.entity())
                .headers(headers -> headers.setBearerAuth(token(identity)))
                .retrieve()
                .body(METADATA_ENVELOPE));
        return unwrap(response);
    }

    public List<EventData> events(String recordId, RemoteIdentity identity) {
        ApiEnvelope<List<Map<String, Object>>> response = apiCall(() -> http.get()
                .uri(dataPath() + "/{id}/events", recordId)
                .headers(headers -> headers.setBearerAuth(token(identity)))
                .retrieve()
                .body(LIST_ENVELOPE));
        List<EventData> events = new ArrayList<>();
        for (Map<String, Object> item : unwrap(response)) {
            events.add(new EventData(
                    string(item.get("id")),
                    string(item.get("action")),
                    string(item.get("actor")),
                    nullableString(item.get("from_status")),
                    nullableString(item.get("to_status")),
                    nullableString(item.get("note")),
                    instant(item.get("at"))
            ));
        }
        return List.copyOf(events);
    }

    private String token(RemoteIdentity identity) {
        CachedToken cached = tokens.get(identity);
        Instant now = Instant.now();
        if (cached != null && cached.refreshAt().isAfter(now)) {
            return cached.value();
        }
        synchronized (tokens) {
            cached = tokens.get(identity);
            now = Instant.now();
            if (cached != null && cached.refreshAt().isAfter(now)) {
                return cached.value();
            }
            DxosApprovalProperties.Identity credentials = switch (identity) {
                case REQUESTER -> properties.requester();
                case APPROVER -> properties.approver();
            };
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("grant_type", "password");
            form.add("client_id", properties.clientId());
            if (!properties.clientSecret().isBlank()) {
                form.add("client_secret", properties.clientSecret());
            }
            form.add("username", credentials.username());
            form.add("password", credentials.password());
            Map<String, Object> response = tokenCall(() -> http.post()
                    .uri("/realms/{realm}/protocol/openid-connect/token", properties.realm())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(MAP_BODY));
            String accessToken = nullableString(response == null ? null : response.get("access_token"));
            if (accessToken == null || accessToken.isBlank()) {
                throw providerUnavailable("DX-OS không trả access token");
            }
            long expiresIn = number(response.get("expires_in"), 60L);
            long cacheSeconds = Math.max(5L, expiresIn - Math.min(30L, expiresIn / 2));
            tokens.put(identity, new CachedToken(accessToken, now.plusSeconds(cacheSeconds)));
            return accessToken;
        }
    }

    private <T> T apiCall(Supplier<T> request) {
        try {
            return request.get();
        } catch (RestClientResponseException ex) {
            int status = ex.getStatusCode().value();
            if (status == 403) {
                throw new AccessDeniedException("DX-OS từ chối thao tác");
            }
            if (status == 404) {
                throw new ResourceNotFoundException("Không tìm thấy dữ liệu xét duyệt trên DX-OS");
            }
            if (status == 409) {
                throw conflict("Workflow DX-OS đã thay đổi; hãy tải lại dữ liệu");
            }
            if (status == 400 || status == 422) {
                throw new ApiDomainException(
                        ApiErrorCode.INVALID_REQUEST,
                        HttpStatus.BAD_REQUEST,
                        "DX-OS từ chối dữ liệu workflow"
                );
            }
            if (status == 401 || status >= 500) {
                throw providerUnavailable("Không thể xác thực hoặc gọi DX-OS");
            }
            throw providerUnavailable("DX-OS trả trạng thái không hỗ trợ: " + status);
        } catch (RestClientException ex) {
            throw providerUnavailable("Không thể kết nối DX-OS");
        }
    }

    private <T> T tokenCall(Supplier<T> request) {
        try {
            return request.get();
        } catch (RestClientResponseException ex) {
            throw providerUnavailable("DX-OS từ chối credential tích hợp");
        } catch (RestClientException ex) {
            throw providerUnavailable("Không thể kết nối dịch vụ định danh DX-OS");
        }
    }

    private <T> T unwrap(ApiEnvelope<T> response) {
        if (response == null || !response.success() || response.data() == null) {
            throw providerUnavailable("DX-OS trả response không hợp lệ");
        }
        return response.data();
    }

    private RecordData record(Map<String, Object> data) {
        return new RecordData(
                string(data.get("id")),
                string(data.get("status")),
                number(data.get("version"), 0L),
                nullableString(data.get("created_by")),
                instant(data.get("created_at")),
                nullableString(data.get("decided_by")),
                nullableInstant(data.get("decided_at"))
        );
    }

    private String dataPath() {
        return "/api/v1/data/" + properties.model() + "/" + properties.entity();
    }

    private static RestClient client(DxosApprovalProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.connectTimeout());
        requestFactory.setReadTimeout(properties.readTimeout());
        return RestClient.builder()
                .baseUrl(properties.normalizedBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    private static String string(Object value) {
        String text = nullableString(value);
        if (text == null || text.isBlank()) {
            throw new IllegalStateException("DX-OS trả thiếu trường bắt buộc");
        }
        return text;
    }

    private static String nullableString(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return Long.toString(number.longValue());
        }
        return value.toString();
    }

    private static long number(Object value, long fallback) {
        if (value == null) {
            return fallback;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("DX-OS trả version không hợp lệ", ex);
        }
    }

    private static Instant instant(Object value) {
        Instant result = nullableInstant(value);
        if (result == null) {
            throw new IllegalStateException("DX-OS trả thiếu timestamp bắt buộc");
        }
        return result;
    }

    private static Instant nullableInstant(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Instant instant) {
            return instant;
        }
        return Instant.parse(value.toString());
    }

    private ApiDomainException providerUnavailable(String message) {
        return new ApiDomainException(
                ApiErrorCode.APPROVAL_PROVIDER_UNAVAILABLE,
                HttpStatus.SERVICE_UNAVAILABLE,
                message
        );
    }

    private ApiDomainException conflict(String message) {
        return new ApiDomainException(ApiErrorCode.WORKFLOW_CONFLICT, HttpStatus.CONFLICT, message);
    }

    public enum RemoteIdentity {
        REQUESTER,
        APPROVER
    }

    public record RecordData(
            String id,
            String status,
            long version,
            String createdBy,
            Instant createdAt,
            String decidedBy,
            Instant decidedAt
    ) { }

    public record EventData(
            String id,
            String action,
            String actor,
            String fromState,
            String toState,
            String note,
            Instant at
    ) { }

    public record EntityMetadata(ProcessMetadata process, List<String> handlerActions) {
        public EntityMetadata {
            handlerActions = handlerActions == null ? List.of() : List.copyOf(handlerActions);
        }
    }

    public record ProcessMetadata(List<StateMetadata> states, List<ActionMetadata> actions) {
        public ProcessMetadata {
            states = states == null ? List.of() : List.copyOf(states);
            actions = actions == null ? List.of() : List.copyOf(actions);
        }
    }

    public record StateMetadata(String name, String label, boolean terminal) { }

    public record ActionMetadata(
            String name,
            String label,
            List<String> from,
            String to,
            String by,
            boolean needsNote
    ) {
        public ActionMetadata {
            from = from == null ? List.of() : List.copyOf(from);
        }
    }

    private record ApiEnvelope<T>(boolean success, String message, T data, String errorCode) { }

    private record CachedToken(String value, Instant refreshAt) { }
}
