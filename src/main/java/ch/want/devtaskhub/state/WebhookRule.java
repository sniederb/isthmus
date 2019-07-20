/*
 * Created on 27 Jul 2018
 */
package ch.want.devtaskhub.state;

import ch.want.devtaskhub.mvc.WebhookController;

public class WebhookRule extends AbstractRule {

    private String hookname;

    /**
     * The path variable this rule will react to
     *
     * @see WebhookController
     */
    public String getHookname() {
        return hookname;
    }

    public void setHookname(final String hookname) {
        this.hookname = hookname;
    }
}
