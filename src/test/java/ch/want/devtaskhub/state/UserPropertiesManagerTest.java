package ch.want.devtaskhub.state;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ch.want.devtaskhub.AbstractSpringConfiguredTest;

class UserPropertiesManagerTest extends AbstractSpringConfiguredTest {

    @Autowired
    private UserProperties userProperties;
    @Autowired
    private UserPropertiesManager userPropertiesManager;

    @Test
    void postConstruct() {
        assertEquals("123456", userProperties.getLicenseKey());
    }

    @Test
    void writePropertiesToFile() {
        userProperties.setLicenseKey("123456");
        userPropertiesManager.writePropertiesToFile();
    }
}
