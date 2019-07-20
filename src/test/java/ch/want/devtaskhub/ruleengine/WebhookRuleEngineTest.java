/*
 * Created on 27 Jul 2018
 */
package ch.want.devtaskhub.ruleengine;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import ch.want.devtaskhub.AbstractSpringConfiguredTest;
import ch.want.devtaskhub.state.Endpoint;
import ch.want.devtaskhub.state.FilterExpressionType;
import ch.want.devtaskhub.state.UserProperties;
import ch.want.devtaskhub.state.WebhookRule;

public class WebhookRuleEngineTest extends AbstractSpringConfiguredTest {

    private static final String HOOKNAME = "junittest";
    @Autowired
    private WebhookRuleEngine testee;
    @Autowired
    private UserProperties userProperties;
    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    public void resetLastExchangeBody() {
        RestClientStub.resetLastExchange();
    }

    @AfterEach
    public void removeWebhookRule() {
        userProperties.getWebhookRules().removeIf(r -> HOOKNAME.equals(r.getHookname()));
    }

    @Test
    public void process_regExpMismatch() throws IOException {
        // arrange
        final WebhookRule rule = addWebhookRule();
        rule.setFilterExpressionType(FilterExpressionType.REGEXP);
        rule.setFilterExpression("jenkins");
        // act
        testee.process(HOOKNAME, buildIncomingJsonBody());
        // assert
    }

    @Test
    public void process_regExpMatch() throws IOException {
        // arrange
        final WebhookRule rule = addWebhookRule();
        rule.setFilterExpressionType(FilterExpressionType.REGEXP);
        rule.setFilterExpression(".*field1.*");
        // act
        testee.process(HOOKNAME, buildIncomingJsonBody());
        // assert
        final HttpEntity<?> sentBody = RestClientStub.getLastExchangeBody();
        assertNotNull(sentBody);
        assertEquals("<{},{Content-Type=[application/json]}>", sentBody.toString(), "HTTP body sent");
    }

    @Test
    public void process_jsonPointerMisMatch() throws IOException {
        // arrange
        final WebhookRule rule = addWebhookRule();
        rule.setFilterExpressionType(FilterExpressionType.JSONPOINTER);
        rule.setFilterExpression("/notthere");
        // act
        testee.process(HOOKNAME, buildIncomingJsonBody());
        // assert
    }

    @Test
    public void process_jsonPointerMatch() throws IOException {
        // arrange
        final WebhookRule rule = addWebhookRule();
        rule.setFilterExpressionType(FilterExpressionType.JSONPOINTER);
        rule.setFilterExpression("/field1");
        // act
        testee.process(HOOKNAME, buildIncomingJsonBody());
        // assert
        final HttpEntity<?> sentBody = RestClientStub.getLastExchangeBody();
        assertNotNull(sentBody);
        assertEquals("<{},{Content-Type=[application/json]}>", sentBody.toString(), "HTTP body sent");
    }

    @Test
    public void process_jenkinsUpdate() throws IOException {
        // arrange
        final WebhookRule rule = addWebhookRule();
        rule.setFilterExpressionType(FilterExpressionType.JSONPOINTER);
        rule.setFilterExpression("");
        rule.setPayloadPath("");
        rule.setPayloadTemplate("http://localhost:8081/jira/rest/api/2/issue/${key}");
        // act
        testee.process(HOOKNAME, buildIncomingJsonFromJenkins());
        // assert
        final HttpEntity<?> sentBody = RestClientStub.getLastExchangeBody();
        assertNotNull(sentBody);
        assertEquals("<http://localhost:8081/jira/rest/api/2/issue/FUN-181:,{Content-Type=[text/plain]}>", sentBody.toString(), "HTTP body sent");
    }

    @Test
    public void process_usingSubPath() throws IOException {
        // arrange
        final WebhookRule rule = addWebhookRule();
        rule.setPayloadPath("/nested");
        rule.setPayloadTemplate("{\"bla\":\"${elementName}\"}");
        // act
        testee.process(HOOKNAME, buildIncomingJsonBody());
        // assert
        final HttpEntity<?> sentBody = RestClientStub.getLastExchangeBody();
        assertNotNull(sentBody);
        assertEquals("<{\"bla\":\"nestedFoo\"},{Content-Type=[application/json]}>", sentBody.toString(), "HTTP body sent");
    }

    @Test
    public void process_usingArrayPath() throws IOException {
        // arrange
        final WebhookRule rule = addWebhookRule();
        rule.setPayloadPath("/elements");
        rule.setPayloadTemplate("{\"bla\":\"${elementName}\"}");
        // act
        testee.process(HOOKNAME, buildIncomingJsonBody());
        // assert
        final HttpEntity<?> sentBody = RestClientStub.getLastExchangeBody();
        assertNotNull(sentBody);
        assertEquals("<{\"bla\":\"foo2\"},{Content-Type=[application/json]}>", sentBody.toString(), "HTTP body sent");
    }

    @Test
    public void process_gitUpdateUsingRegExpPath() throws IOException {
        // arrange
        final WebhookRule rule = addWebhookRule();
        rule.setFilterExpressionType(FilterExpressionType.REGEXP);
        rule.setFilterExpression(".*FUN-.*");
        rule.setPayloadPath(".*?: ([\\w-]+).*");
        final String jiraApiTemplate = "{\n" +
                "        \"update\": {\n" +
                "                \"labels\": [{\n" +
                "                        \"add\": \"code-changes\"\n" +
                "                },\n" +
                "                {\n" +
                "                        \"remove\": \"deployed\"\n" +
                "                }]\n" +
                "        }\n" +
                "}";
        rule.setPayloadTemplate(jiraApiTemplate);
        // act
        testee.process(HOOKNAME, buildIncomingPlaintextBody());
        // assert
        final HttpEntity<?> sentBody = RestClientStub.getLastExchangeBody();
        assertNotNull(sentBody);
        assertEquals("<{\"update\":{\"labels\":[{\"add\":\"code-changes\"},{\"remove\":\"deployed\"}]}},{Content-Type=[application/json]}>",
                sentBody.toString(), "HTTP body sent");
    }

    public WebhookRule addWebhookRule() {
        final WebhookRule rule = new WebhookRule();
        rule.setHookname(HOOKNAME);
        rule.setEndpoint(new Endpoint("localhost:8083/jira/rest/api", HttpMethod.PUT.toString(), "", ""));
        rule.setPayloadTemplate("{}");
        userProperties.getWebhookRules().add(rule);
        return rule;
    }

    private String buildIncomingJsonBody() {
        final ObjectNode rootNode = objectMapper.createObjectNode();
        rootNode.set("field1", new TextNode("value1"));
        rootNode.set("field2", new TextNode("value2"));
        rootNode.set("field3", new TextNode("value3"));
        final ArrayNode arrayNode = objectMapper.createArrayNode();
        for (int i = 0; i < 3; i++) {
            final ObjectNode elementNode = objectMapper.createObjectNode();
            elementNode.set("elementName", new TextNode("foo" + i));
            elementNode.set("elementValue", new TextNode("bar" + i));
            arrayNode.add(elementNode);
        }
        rootNode.set("elements", arrayNode);
        final ObjectNode nestedNode = objectMapper.createObjectNode();
        nestedNode.set("elementName", new TextNode("nestedFoo"));
        nestedNode.set("elementValue", new TextNode("nestedBar"));
        rootNode.set("nested", nestedNode);
        return rootNode.toString();
    }

    private String buildIncomingPlaintextBody() {
        return "c1229d8 Simon Niederberger: FUN-168: Trip UI: Display travel services\n- Working on display of raw sources\nTask-Url: https://www.want.ch/jira/browse/FUN-188";
    }

    private String buildIncomingJsonFromJenkins() {
        return "{\"key\":\"FUN-181:\",\"message\":\"FUN-181: Errors in Jenkinsfile\",\"author\":\"Simon Niederberger\"}";
    }
}
