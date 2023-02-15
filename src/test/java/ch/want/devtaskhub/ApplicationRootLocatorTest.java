package ch.want.devtaskhub;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ApplicationRootLocatorTest extends AbstractSpringConfiguredTest {

    @Test
    void getPropertyValue() {
        final String appRoot = new ApplicationRootLocator().getPropertyValue();
        assertNotNull(appRoot);
    }
}
