package ch.want.devtaskhub.ruleengine;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StreamUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.want.devtaskhub.AbstractSpringConfiguredTest;

public class TemplateEngineTest extends AbstractSpringConfiguredTest {

    @Autowired
    private TemplateEngine testee;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void render_jsonStylePayload() throws Exception {
        // arrange
        final Map<String, Object> payloadModel = new HashMap<>();
        payloadModel.put("title", "Hello World");
        final String template = "{\"query\": \"${title}\"}";
        // act
        final String content = testee.render(template, payloadModel);
        // arrange
        assertEquals("{\"query\": \"Hello World\"}", content);
    }

    @Test
    public void render_regExpStylePayload() throws Exception {
        // arrange
        final Map<String, Object> payloadModel = new HashMap<>();
        payloadModel.put("group1", "JIRA-155");
        final String template = "http://localhost:8080/item/${group1}";
        // act
        final String content = testee.render(template, payloadModel);
        // arrange
        assertEquals("http://localhost:8080/item/JIRA-155", content);
    }

    @Test
    public void render_jiraResponse() throws Exception {
        final String payload = StreamUtils.copyToString(this.getClass().getResourceAsStream("/ch/want/devtaskhub/jira/search-issues-response.json"),
                Charset.forName("UTF-8"));
        final String template = StreamUtils.copyToString(this.getClass().getResourceAsStream("/ch/want/devtaskhub/slack/list_of_issues.freemarker.template"),
                Charset.forName("UTF-8"));
        final JsonNode payloadNode = objectMapper.readTree(payload);
        final Map<String, Object> payloadModel = objectMapper.convertValue(payloadNode, new TypeReference<Map<String, Object>>() {
        });
        // act
        final String content = testee.render(template, payloadModel);
        // arrange
        MatcherAssert.assertThat(content, CoreMatchers.containsString("FUN-176 - Switching locale in endless-loop"));
    }
}
