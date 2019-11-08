package ch.want.devtaskhub.mvc;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import ch.want.devtaskhub.ruleengine.WebhookRuleEngine;
import ch.want.devtaskhub.state.ApplicationState;

@RestController
public class WebhookController {

    private static final Logger LOG = LoggerFactory.getLogger(WebhookController.class);
    @Autowired
    private ApplicationState applicationState;
    @Autowired
    private WebhookRuleEngine ruleEngine;

    @RequestMapping(value = "/webhooks/{hookname}", method = { RequestMethod.POST, RequestMethod.PUT })
    public void onIncomingWebhook(@PathVariable final String hookname, @RequestBody final String payload) throws IOException {
        logPayloadIfEnabled(hookname, payload);
        ruleEngine.process(hookname, payload);
    }

    private void logPayloadIfEnabled(final String hookname, final String payload) {
        if (Boolean.parseBoolean(System.getProperty("isthmus.logincoming", "false"))) {
            LOG.info("Received payload on {}: {}", hookname, payload);
            applicationState.addAction("Received payload on /webhooks/" + hookname + ": " + payload);
        } else {
            applicationState.addAction("Received notification on /webhooks/" + hookname);
        }
    }
}
