package ch.want.devtaskhub.state;

public class ScheduledRule extends AbstractRule {

    private String cronExpression;
    private String scheduledPayload;
    private Endpoint scheduledQueryEndpoint;

    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(final String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public String getScheduledPayload() {
        return scheduledPayload;
    }

    public void setScheduledPayload(final String scheduledPayload) {
        this.scheduledPayload = scheduledPayload;
    }

    public Endpoint getScheduledQueryEndpoint() {
        return scheduledQueryEndpoint;
    }

    public void setScheduledQueryEndpoint(final Endpoint scheduledQueryEndpoint) {
        this.scheduledQueryEndpoint = scheduledQueryEndpoint;
    }
}
