/*
 * Created on 22 Jul 2018
 */
package ch.want.devtaskhub;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class ApplicationRootLocatorTest extends AbstractSpringConfiguredTest {

    @Test
    public void getPropertyValue() {
        final String appRoot = new ApplicationRootLocator().getPropertyValue();
        assertNotNull(appRoot);
    }
}
