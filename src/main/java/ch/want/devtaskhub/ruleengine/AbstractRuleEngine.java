/*
 * Created on 27 Jul 2018
 */
package ch.want.devtaskhub.ruleengine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import ch.want.devtaskhub.common.RestClient;
import ch.want.devtaskhub.common.RestClientFactory;
import ch.want.devtaskhub.licensing.LicenseClient;
import ch.want.devtaskhub.state.AbstractRule;
import ch.want.devtaskhub.state.ApplicationState;
import ch.want.devtaskhub.state.UserProperties;

public class AbstractRuleEngine {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractRuleEngine.class);
    @Autowired
    protected UserProperties userProperties;
    @Autowired
    protected TemplateEngine templateEngine;
    @Autowired
    protected ObjectMapper objectMapper;
    @Autowired
    protected RestClientFactory restClientFactory;
    @Autowired
    private LicenseClient licenseClient;
    @Autowired
    protected ApplicationState applicationState;

    protected void extractPayloadAndSend(final AbstractRule rule, final String incomingContent) throws IOException {
        if (!isIncomingFilterExpressionMatch(rule, incomingContent)) {
            applicationState.addAction("Discarded payload as it didn't match " + rule.getFilterExpression());
            return;
        }
        final List<Map<String, Object>> outgoingPayload = getPayloadsFromIncomingBody(rule, incomingContent);
        if (!outgoingPayload.isEmpty()) {
            sendPayload(rule, outgoingPayload);
        } else {
            applicationState.addAction("Incoming payload matched filter, but mapped root " + rule.getPayloadPath() + " is empty");
        }
    }

    private void sendPayload(final AbstractRule rule, final List<Map<String, Object>> outgoingPayload) throws IOException {
        if (!licenseClient.hasValidLicense()) {
            applicationState.addAction("No valid license, so payload will not be sent");
            return;
        }
        final RestClient restClient = restClientFactory.getClient(rule.getEndpoint());
        outgoingPayload.stream().forEach(m -> {
            try {
                sendPayload(rule, m, restClient);
            } catch (final IOException e) {
                LOG.error(e.getMessage(), e);
            }
        });
    }

    private void sendPayload(final AbstractRule rule, final Map<String, Object> templateData, final RestClient restClient) throws IOException {
        final String renderedTemplate = templateEngine.render(rule.getPayloadTemplate(), templateData);
        final HttpEntity<?> httpBody = toHttpEntity(renderedTemplate);
        restClient.renderUrl(templateData, this.templateEngine);
        try {
            if (HttpMethod.PUT.matches(rule.getEndpoint().getHttpMethod())) {
                restClient.put(httpBody);
            } else if (HttpMethod.POST.matches(rule.getEndpoint().getHttpMethod())) {
                restClient.post(httpBody);
            } else {
                throw new IllegalArgumentException("Only supporting methods POST and PUT");
            }
            applicationState.addAction("Sent " + rule.getEndpoint().getHttpMethod() + " to " + restClient.getCurrentRenderedUrl());
        } catch (final Exception e) {
            applicationState.addAction(
                    "Failed to send message " + httpBody.toString() + " to URL [" + restClient.getCurrentRenderedUrl() + "], error was: " + e.getMessage());
            LOG.error("Failed to send HTTP body [{}]", httpBody.toString(), e);
        }
    }

    private HttpEntity<?> toHttpEntity(final String renderedTemplate) {
        try {
            final JsonNode jsonPayload = objectMapper.readTree(renderedTemplate);
            final HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            return new HttpEntity<>(jsonPayload, headers);
        } catch (final IOException ex) { // NOSONAR
            if (renderedTemplate.startsWith("{")) {
                // this might've been a JSON template, so let user know what's wrong
                LOG.warn(ex.getMessage(), ex);
                applicationState.addAction("Failed to parse template as JSON: " + ex.getMessage());
            }
            final HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);
            return new HttpEntity<>(renderedTemplate, headers);
        }
    }

    /**
     * Get a list of matches, each mapped as key/value pairs.
     *
     * @param rule
     * @param incomingJson
     * @return
     */
    private List<Map<String, Object>> getPayloadsFromIncomingBody(final AbstractRule rule, final String incomingContent) {
        return tryReadTree(incomingContent)
                .map(js -> getPayloadsFromIncomingBody(rule, js))
                .orElseGet(() -> {
                    if (StringUtils.isBlank(rule.getPayloadPath())) {
                        throw new IllegalArgumentException("Text payload MUST have a RegExp defined as payload path");
                    }
                    final Matcher matcher = Pattern.compile(rule.getPayloadPath(), Pattern.DOTALL).matcher(incomingContent);
                    return buildMapFromRegExp(matcher);
                });
    }

    private List<Map<String, Object>> getPayloadsFromIncomingBody(final AbstractRule rule, final JsonNode incomingContent) {
        JsonNode jsonPayload = incomingContent;
        if (!StringUtils.isBlank(rule.getPayloadPath())) {
            // for JSON, assume a JSON Pointer expression
            jsonPayload = jsonPayload.at(rule.getPayloadPath());
        }
        if ((jsonPayload != null) && (!jsonPayload.isMissingNode())) {
            return buildMapFromJson(jsonPayload);
        }
        return Collections.emptyList();
    }

    private Optional<JsonNode> tryReadTree(final String incomingContent) {
        final boolean looksLikeJson = StringUtils.startsWithAny(incomingContent.trim(), "{", "[") && StringUtils.endsWithAny(incomingContent.trim(), "}", "]");
        if (looksLikeJson) {
            try {
                return Optional.of(objectMapper.readTree(incomingContent));
            } catch (final IOException ex) { // NOSONAR
            }
        }
        return Optional.empty();
    }

    private List<Map<String, Object>> buildMapFromJson(final JsonNode jsonPayload) {
        final List<Map<String, Object>> result = new ArrayList<>();
        if (jsonPayload.isArray()) {
            IteratorUtils.toList(((ArrayNode) jsonPayload).elements()).stream()//
                    .forEach(subnode -> result.addAll(buildMapFromJson(subnode)));
        } else {
            final Map<String, Object> payloadModel = objectMapper.convertValue(jsonPayload, new TypeReference<Map<String, Object>>() {
            });
            result.add(payloadModel);
        }
        return result;
    }

    private List<Map<String, Object>> buildMapFromRegExp(final Matcher matcher) {
        final List<Map<String, Object>> result = new ArrayList<>();
        while (matcher.find()) {
            final Map<String, Object> mapEntry = new HashMap<>();
            for (int i = 0; i <= matcher.groupCount(); i++) {
                mapEntry.put("group" + i, matcher.group(i));
            }
            result.add(mapEntry);
        }
        return result;
    }

    private boolean isIncomingFilterExpressionMatch(final AbstractRule rule, final String incomingBody) throws IOException {
        if (StringUtils.isBlank(rule.getFilterExpression())) {
            return true;
        }
        switch (rule.getFilterExpressionType()) {
        case JSONPOINTER:
            final JsonNode incomingJson = objectMapper.readTree(incomingBody);
            final JsonNode nodeNeedsToBePresent = incomingJson.at(rule.getFilterExpression());
            return nodeNeedsToBePresent != null && !nodeNeedsToBePresent.isMissingNode();
        case REGEXP:
            return Pattern.compile(rule.getFilterExpression(), Pattern.DOTALL).matcher(incomingBody).find();
        }
        return StringUtils.isNotBlank(incomingBody);
    }
}
