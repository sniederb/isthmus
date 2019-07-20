/*
 * Created on 18 Jul 2018
 */
package ch.want.devtaskhub.common;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.codec.Charsets;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.want.devtaskhub.ruleengine.TemplateEngine;
import ch.want.devtaskhub.state.Endpoint;

/**
 * HTTP(S) REST client, NOT thread-safe.
 */
public class RestClient {

    private static final Logger LOG = LoggerFactory.getLogger(RestClient.class);
    private final HttpComponentsClientHttpRequestFactory requestFactory;
    protected final ObjectMapper mapper = new ObjectMapper();
    private final String basicAuthorization;
    /**
     * Possibly a Freemarker template
     */
    private final String urlTemplate;
    private String renderedUrl;

    public RestClient(final Endpoint endpoint) {
        this(endpoint.getUrl(), endpoint.getUsername(), endpoint.getPassword());
    }

    /**
     * Create a client with a 'Basic Authorization' mechanism
     */
    private RestClient(final String baseUrl, final String username, final String password) {
        this.urlTemplate = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.renderedUrl = this.urlTemplate;
        if (StringUtils.isNoneBlank(username, password)) {
            this.basicAuthorization = "Basic " + new String(Base64.encodeBase64((username + ":" + password).getBytes()));
        } else {
            this.basicAuthorization = null;
        }
        requestFactory = new HttpComponentsClientHttpRequestFactory();
        requestFactory.setHttpClient(HttpClients.custom()
                .setSSLHostnameVerifier(new NoopHostnameVerifier())
                .build());
    }

    public RestClient renderUrl(final Map<String, Object> templateData, final TemplateEngine templateEngine) throws IOException {
        // if this becomes a performance issue, add a
        // test whether or not urlTemplate is a template
        // or a plain URL
        this.renderedUrl = templateEngine.render(this.urlTemplate, templateData);
        return this;
    }

    public String getCurrentRenderedUrl() {
        return this.renderedUrl;
    }

    public <T> Optional<T> get(final Class<T> responseClass) throws IOException {
        return get(null, null, responseClass);
    }

    public <T> Optional<T> get(final Map<String, String> parameters, final Class<T> responseClass)
            throws IOException {
        return get(parameters, null, responseClass);
    }

    public <T> Optional<T> get(final Map<String, String> parameters, final HttpHeaders headers, final Class<T> responseClass)
            throws IOException {
        String subPath = "";
        if (parameters != null) {
            subPath += "?" + parameters.entrySet().stream()//
                    .map(e -> (e.getKey() + "=" + e.getValue()).replaceAll("&", "&amp;"))
                    .collect(Collectors.joining("&"));
        }
        final HttpEntity<?> entity = Optional.ofNullable(headers).map(h -> new HttpEntity<>(headers)).orElse(null);
        return exchange(subPath, HttpMethod.GET, entity, responseClass);
    }

    public Optional<JsonNode> post(final HttpEntity<?> body) {
        return exchange("", HttpMethod.POST, body, JsonNode.class);
    }

    public Optional<JsonNode> put(final HttpEntity<?> body) {
        return exchange("", HttpMethod.PUT, body, JsonNode.class);
    }

    @SuppressWarnings("unchecked")
    protected <T> Optional<T> exchange(final String urlQuery, final HttpMethod method, final HttpEntity<?> body, final Class<T> responseClass) {
        if (JsonNode.class.equals(responseClass)) {
            return (Optional<T>) exchange(urlQuery, method, body, String.class).map(this::coerceStringToJson);
        }
        final RestTemplate restTemplate = new RestTemplate(requestFactory);
        restTemplate.setErrorHandler(new LoggingErrorHandler());
        final ResponseEntity<T> response = restTemplate
                .exchange(getEndpoint(urlQuery), method, addAuthorization(body), responseClass);
        return Optional.ofNullable(response.getBody());
    }

    protected String getEndpoint(final String urlQuery) {
        return this.renderedUrl + (urlQuery == null ? "" : urlQuery);
    }

    /**
     * Slack will reply with a simple 'ok' on a webhook POST. This method attempts to fit such responses into a JSON
     * format.
     *
     * @param s
     * @return
     */
    private JsonNode coerceStringToJson(final String s) {
        if (StringUtils.isBlank(s)) {
            return null;
        }
        String jsonString = s;
        if (!jsonString.startsWith("{")) {
            jsonString = "{\"response\":\"" + s.replace('"', '\'') + "\"}";
        }
        try {
            return mapper.readTree(jsonString);
        } catch (final IOException e) {
            LOG.warn("Failed to parse to JSON: {}", jsonString);
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    private HttpEntity<?> addAuthorization(final HttpEntity<?> body) {
        if (StringUtils.isBlank(basicAuthorization)) {
            return body;
        }
        final HttpHeaders headers = new HttpHeaders();
        if (body == null) {
            headers.set(HttpHeaders.AUTHORIZATION, basicAuthorization);
            return new HttpEntity<>(headers);
        }
        headers.addAll(body.getHeaders());
        headers.set(HttpHeaders.AUTHORIZATION, basicAuthorization);
        return new HttpEntity<>(body.getBody(), headers);
    }

    private static class LoggingErrorHandler implements ResponseErrorHandler {

        @Override
        public boolean hasError(final ClientHttpResponse response) throws IOException {
            return !response.getStatusCode().is2xxSuccessful();
        }

        @Override
        public void handleError(final ClientHttpResponse response) throws IOException {
            if (response.getBody() != null) {
                final String bodyContent = StreamUtils.copyToString(response.getBody(), Charsets.ISO_8859_1);
                LOG.warn(bodyContent);
            }
            throw new IllegalStateException(response.getStatusCode().toString() + " - " + response.getStatusText());
        }
    }
}
