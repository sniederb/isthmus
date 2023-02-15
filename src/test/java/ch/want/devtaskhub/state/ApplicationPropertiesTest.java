package ch.want.devtaskhub.state;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ch.want.devtaskhub.AbstractSpringConfiguredTest;

class ApplicationPropertiesTest extends AbstractSpringConfiguredTest {

    @Autowired
    private ApplicationProperties testee;

    @Test
    void getUserpropertiesPath_notNull() throws Exception {
        assertNotNull(testee.getUserpropertiesPath());
    }
}
