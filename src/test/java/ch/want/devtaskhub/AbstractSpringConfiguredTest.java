package ch.want.devtaskhub;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ch.want.devtaskhub.state.Endpoint;
import ch.want.devtaskhub.state.FilterExpressionType;
import ch.want.devtaskhub.state.UserProperties;
import ch.want.devtaskhub.state.UserPropertiesManager;
import ch.want.devtaskhub.state.WebhookRule;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles(profiles = { "test", "workspace" })
public abstract class AbstractSpringConfiguredTest {

    @Autowired
    private UserProperties userProperties;
    @Autowired
    private UserPropertiesManager userPropertiesManager;

    @BeforeEach
    public void resetSettings() {
        final UserProperties defaultUserProperties = getDefaultUserProperties();
        userProperties.copyFrom(defaultUserProperties);
        userPropertiesManager.writePropertiesToFile();
    }

    private UserProperties getDefaultUserProperties() {
        final UserProperties result = new UserProperties();
        result.setEmail("foobar@isthmus.want.ch");
        result.setLicenseKey("123456");
        result.setUsername("admin");
        result.setPassword("admin");
        result.getWebhookRules().add(getDefaultWebhookRule());
        return result;
    }

    private WebhookRule getDefaultWebhookRule() {
        final WebhookRule result = new WebhookRule();
        result.setHookname("jenkins");
        result.setPayloadPath("");
        result.setEndpoint(new Endpoint("http://localhost:8080/webhooks/other", "POST", "simon", "hello"));
        result.setFilterExpression("");
        result.setFilterExpressionType(FilterExpressionType.REGEXP);
        return result;
    }
}
