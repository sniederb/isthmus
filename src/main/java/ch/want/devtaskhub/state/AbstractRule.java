package ch.want.devtaskhub.state;

public abstract class AbstractRule {

    private String title;
    private String payloadTemplate;
    private String payloadPath;
    private String filterExpression;
    private FilterExpressionType filterExpressionType;
    private Endpoint endpoint;
    private boolean enabled = true;

    public String getPayloadTemplate() {
        return payloadTemplate;
    }

    public void setPayloadTemplate(final String payloadTemplate) {
        this.payloadTemplate = payloadTemplate;
    }

    public Endpoint getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(final Endpoint endpoint) {
        this.endpoint = endpoint;
    }

    /**
     * Either a JSON Pointer or RegExp
     *
     * @return
     */
    public String getPayloadPath() {
        return payloadPath;
    }

    public void setPayloadPath(final String payloadPath) {
        this.payloadPath = payloadPath;
    }

    public String getFilterExpression() {
        return filterExpression;
    }

    public void setFilterExpression(final String filterExpression) {
        this.filterExpression = filterExpression;
    }

    public FilterExpressionType getFilterExpressionType() {
        return filterExpressionType;
    }

    public void setFilterExpressionType(final FilterExpressionType filterExpressionType) {
        this.filterExpressionType = filterExpressionType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }
}
