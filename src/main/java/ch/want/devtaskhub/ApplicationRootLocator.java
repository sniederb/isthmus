package ch.want.devtaskhub;

import org.springframework.boot.system.ApplicationHome;

import ch.qos.logback.core.PropertyDefinerBase;

/**
 * Get the file location of the class root.
 */
public class ApplicationRootLocator extends PropertyDefinerBase {

    @Override
    public String getPropertyValue() {
        final ApplicationHome home = new ApplicationHome(this.getClass());
        return home.getDir().getAbsolutePath();
    }
}
