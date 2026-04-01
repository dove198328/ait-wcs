package cn.aitplus.wcs.adapters.io.http;

import cn.aitplus.wcs.core.domain.enums.DomainEnums;
import cn.aitplus.wcs.core.spi.device.DeviceEndpoint;
import cn.aitplus.wcs.core.spi.device.DeviceIoItem;
import cn.aitplus.wcs.core.spi.device.DeviceIoRequest;
import cn.aitplus.wcs.core.spi.device.DeviceIoResult;
import cn.aitplus.wcs.core.spi.device.DeviceTransport;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * RCS 域 HTTP 调用：{@code DeviceIoItem.address} 为相对路径（如 {@code /api/task}）；
 * {@code write == true} 时 POST（body 为 {@code value} 的 JSON），否则 GET。
 */
public class HttpDeviceTransport implements DeviceTransport, Ordered {

    private static final Logger LOG = LoggerFactory.getLogger(HttpDeviceTransport.class);

    private static final int MAX_ERROR_BODY_CHARS = 4_096;

    private final HttpAdapterProperties properties;
    private final ObjectMapper objectMapper;

    public HttpDeviceTransport(HttpAdapterProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 30;
    }

    @Override
    public boolean supports(DomainEnums.CommandDomain domain) {
        return domain == DomainEnums.CommandDomain.HTTP;
    }

    @Override
    public DeviceIoResult execute(DeviceIoRequest request) {
        if (request == null || request.getEndpoint() == null) {
            return DeviceIoResult.fail("INVALID_REQUEST", "request or endpoint is null");
        }
        List<DeviceIoItem> items = request.getItems();
        if (items == null || items.isEmpty()) {
            return DeviceIoResult.fail("INVALID_REQUEST", "items is empty");
        }
        String baseUrl = resolveBaseUrl(request.getEndpoint());
        if (!StringUtils.hasText(baseUrl)) {
            return DeviceIoResult.fail("INVALID_ENDPOINT", "httpBaseUrl or host is required for RCS HTTP");
        }
        long timeout = effectiveTimeoutMillis(request.getTimeoutMillis());
        RestTemplate restTemplate = buildRestTemplate(timeout);

        try {
            List<String> bodies = new ArrayList<>(items.size());
            for (DeviceIoItem item : items) {
                if (item == null || !StringUtils.hasText(item.getAddress())) {
                    return DeviceIoResult.fail("INVALID_ADDRESS", "item address is blank");
                }
                URI uri = buildUri(baseUrl, item.getAddress().trim());
                boolean write = Boolean.TRUE.equals(item.getWrite());
                HttpHeaders headers = buildHeaders(request);
                if (write) {
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    String jsonBody = serializeBody(item.getValue());
                    HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
                    ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.POST, entity, String.class);
                    bodies.add(response.getBody() != null ? response.getBody() : "");
                } else {
                    HttpEntity<Void> entity = new HttpEntity<>(headers);
                    ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
                    bodies.add(response.getBody() != null ? response.getBody() : "");
                }
            }
            if (bodies.size() == 1) {
                return DeviceIoResult.ok(bodies.get(0));
            }
            return DeviceIoResult.ok(objectMapper.writeValueAsString(bodies));
        } catch (HttpStatusCodeException e) {
            LOG.warn("RCS HTTP status {} for {}", e.getStatusCode(), safeUriForLog(baseUrl, items), e);
            String snippet = truncateErrorBody(e.getResponseBodyAsString(StandardCharsets.UTF_8));
            String msg = snippet.isEmpty() ? String.valueOf(e.getStatusCode().value()) : e.getStatusCode().value() + ": " + snippet;
            return DeviceIoResult.fail("HTTP_" + e.getStatusCode().value(), msg);
        } catch (RestClientException e) {
            LOG.warn("RCS HTTP client error for {}", safeUriForLog(baseUrl, items), e);
            return DeviceIoResult.fail("HTTP_CLIENT_ERROR",
                e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
        } catch (JsonProcessingException e) {
            LOG.warn("RCS HTTP JSON error", e);
            return DeviceIoResult.fail("HTTP_JSON_ERROR", e.getMessage() != null ? e.getMessage() : "JSON error");
        }
    }

    private String safeUriForLog(String baseUrl, List<DeviceIoItem> items) {
        if (items == null || items.isEmpty()) {
            return baseUrl;
        }
        try {
            return buildUri(baseUrl, items.get(0).getAddress().trim()).toString();
        } catch (Exception ex) {
            return baseUrl;
        }
    }

    private long effectiveTimeoutMillis(long requestTimeout) {
        if (requestTimeout > 0) {
            return requestTimeout;
        }
        return Math.max(1L, properties.getDefaultRequestTimeoutMillis());
    }

    private static RestTemplate buildRestTemplate(long timeoutMillis) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        int t = (int) Math.min(timeoutMillis, Integer.MAX_VALUE);
        factory.setConnectTimeout(t);
        factory.setReadTimeout(t);
        return new RestTemplate(factory);
    }

    static String resolveBaseUrl(DeviceEndpoint endpoint) {
        if (endpoint == null) {
            return null;
        }
        if (StringUtils.hasText(endpoint.getHttpBaseUrl())) {
            String u = endpoint.getHttpBaseUrl().trim();
            while (u.endsWith("/")) {
                u = u.substring(0, u.length() - 1);
            }
            return u;
        }
        if (!StringUtils.hasText(endpoint.getHost())) {
            return null;
        }
        String host = endpoint.getHost().trim();
        int port = endpoint.getPort();
        if (port == 443) {
            return "https://" + host;
        }
        if (port > 0 && port != 80) {
            return "http://" + host + ":" + port;
        }
        return "http://" + host;
    }

    static URI buildUri(String baseUrl, String address) {
        String path = address.trim();
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return UriComponentsBuilder.fromHttpUrl(baseUrl).path(path).build().encode().toUri();
    }

    private HttpHeaders buildHeaders(DeviceIoRequest request) {
        HttpHeaders headers = new HttpHeaders();
        if (StringUtils.hasText(request.getIdempotencyKey())) {
            headers.add("X-Idempotency-Key", request.getIdempotencyKey());
        }
        if (StringUtils.hasText(request.getTraceId())) {
            headers.add("X-Trace-Id", request.getTraceId());
        }
        if (StringUtils.hasText(request.getCorrelationId())) {
            headers.add("X-Correlation-Id", request.getCorrelationId());
        }
        return headers;
    }

    private String serializeBody(Object value) throws JsonProcessingException {
        if (value == null) {
            return "{}";
        }
        if (value instanceof String) {
            String s = ((String) value).trim();
            if (s.isEmpty()) {
                return "{}";
            }
            return s;
        }
        return objectMapper.writeValueAsString(value);
    }

    private static String truncateErrorBody(String body) {
        if (body == null || body.isEmpty()) {
            return "";
        }
        if (body.length() <= MAX_ERROR_BODY_CHARS) {
            return body;
        }
        return body.substring(0, MAX_ERROR_BODY_CHARS) + "...";
    }
}
