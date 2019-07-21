/*
 * Created on 18 Jul 2018
 */
package ch.want.devtaskhub.mvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.nio.charset.Charset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.util.StreamUtils;

import ch.want.devtaskhub.AbstractSpringConfiguredTest;
import ch.want.devtaskhub.ruleengine.RestClientStub;
import ch.want.devtaskhub.state.Endpoint;
import ch.want.devtaskhub.state.UserProperties;
import ch.want.devtaskhub.state.WebhookRule;

public class WebhookControllerTest extends AbstractSpringConfiguredTest {

    private static final String HOOKNAME = "junittest";
    @Autowired
    private MockMvc mvc;
    @Autowired
    private UserProperties userProperties;

    @BeforeEach
    public void resetLastExchangeBody() {
        RestClientStub.resetLastExchange();
    }

    @Test
    public void onIncomingWebhook() throws Exception {
        final WebhookRule rule = addWebhookRule();
        rule.setPayloadPath("/build");
        rule.setPayloadTemplate("{\"jobStatus\":\"${status}\"}");
        final String payload = StreamUtils.copyToString(this.getClass().getResourceAsStream("/ch/want/devtaskhub/jenkins/notification-example.json"),
                Charset.forName("UTF-8"));
        // act
        mvc.perform(MockMvcRequestBuilders.post("/webhooks/" + HOOKNAME)//
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isOk());
        // assert
        final HttpEntity<?> sentBody = RestClientStub.getLastExchangeBody();
        assertEquals("localhost:8083/jira/rest/api", RestClientStub.getLastUrl());
        assertNotNull(sentBody);
        assertEquals("<{\"jobStatus\":\"FAILURE\"},{Content-Type=[application/json]}>", sentBody.toString(), "HTTP body sent");
    }

    public WebhookRule addWebhookRule() {
        final WebhookRule rule = new WebhookRule();
        rule.setHookname(HOOKNAME);
        rule.setEndpoint(new Endpoint("localhost:8083/jira/rest/api", HttpMethod.PUT.toString(), "", ""));
        rule.setPayloadTemplate("{}");
        rule.setEnabled(true);
        userProperties.getWebhookRules().add(rule);
        return rule;
    }
}
