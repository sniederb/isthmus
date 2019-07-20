package ch.want.devtaskhub.ruleengine;

import java.io.IOException;
import java.util.Optional;

import org.springframework.stereotype.Component;

import ch.want.devtaskhub.state.WebhookRule;

@Component
public class WebhookRuleEngine extends AbstractRuleEngine {

    public void process(final String hookname, final String incomingBody) throws IOException {
        final WebhookRule rule = getRule(hookname).orElseThrow(IllegalArgumentException::new);
        if (rule.isEnabled()) {
            extractPayloadAndSend(rule, incomingBody);
        }
    }

    private Optional<WebhookRule> getRule(final String hookname) {
        return this.userProperties.getWebhookRules().stream() //
                .filter(r -> hookname.equalsIgnoreCase(r.getHookname())) //
                .findFirst();
    }
}