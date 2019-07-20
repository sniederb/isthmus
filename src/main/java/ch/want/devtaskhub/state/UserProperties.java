/*
 * Created on 18 Jul 2018
 */
package ch.want.devtaskhub.state;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

// remember: @Component is a Spring bean and a Singleton
@Component
public class UserProperties {

    private boolean untouched;
    private String licenseKey;
    private String email;
    private String username;
    private String password;
    private List<WebhookRule> webhookRules = new ArrayList<>();
    private List<ScheduledRule> scheduledRules = new ArrayList<>();

    public void copyFrom(final UserProperties otherProperties) {
        this.untouched = otherProperties.untouched;
        this.licenseKey = otherProperties.licenseKey;
        this.email = otherProperties.email;
        this.username = otherProperties.username;
        this.password = otherProperties.password;
        this.webhookRules.clear();
        this.webhookRules.addAll(otherProperties.webhookRules);
        this.scheduledRules.clear();
        this.scheduledRules.addAll(otherProperties.scheduledRules);
    }

    public void copyTo(final UserProperties otherProperties) {
        otherProperties.untouched = this.untouched;
        otherProperties.licenseKey = this.licenseKey;
        otherProperties.email = this.email;
        otherProperties.username = this.username;
        otherProperties.password = this.password;
        otherProperties.webhookRules.clear();
        otherProperties.webhookRules.addAll(this.webhookRules);
        otherProperties.scheduledRules.clear();
        otherProperties.scheduledRules.addAll(this.scheduledRules);
    }

    public String getEmail() {
        return email == null ? "" : email;
    }

    public void setEmail(final String email) {
        this.email = email;
    }

    public String getLicenseKey() {
        return licenseKey == null ? "" : licenseKey;
    }

    public void setLicenseKey(final String licenseKey) {
        this.licenseKey = licenseKey;
    }

    public boolean isUntouched() {
        return untouched;
    }

    public void setUntouched(final boolean untouched) {
        this.untouched = untouched;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    public List<WebhookRule> getWebhookRules() {
        return webhookRules;
    }

    public void setWebhookRules(final List<WebhookRule> webhookRules) {
        this.webhookRules = webhookRules;
    }

    public List<ScheduledRule> getScheduledRules() {
        return scheduledRules;
    }

    public void setScheduledRules(final List<ScheduledRule> scheduledRules) {
        this.scheduledRules = scheduledRules;
    }

    public void validate() {
        getWebhookRules().stream().forEach(UserProperties::validate);
        getScheduledRules().stream().forEach(UserProperties::validate);
    }

    private static void validate(final WebhookRule rule) {
        if (StringUtils.isAnyBlank(rule.getHookname(), rule.getEndpoint().getUrl())) {
            throw new IllegalArgumentException("Missing mandatory fields");
        }
        if (!rule.getPayloadTemplate().startsWith("{")) {
            throw new IllegalArgumentException("Template must be JSON");
        }
    }

    private static void validate(final ScheduledRule rule) {
        // will fail on invalid expression
        new CronTrigger(rule.getCronExpression());
        if (StringUtils.isAnyBlank(rule.getEndpoint().getUrl(), rule.getScheduledQueryEndpoint().getUrl())) {
            throw new IllegalArgumentException("Missing mandatory fields");
        }
        if (!rule.getPayloadTemplate().startsWith("{")) {
            throw new IllegalArgumentException("Template must be JSON");
        }
    }
}