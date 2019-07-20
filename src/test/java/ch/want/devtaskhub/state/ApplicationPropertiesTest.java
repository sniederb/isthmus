/*
 * Created on 17 Jul 2018
 */
package ch.want.devtaskhub.state;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ch.want.devtaskhub.AbstractSpringConfiguredTest;

public class ApplicationPropertiesTest extends AbstractSpringConfiguredTest {

    @Autowired
    private ApplicationProperties testee;

    @Test
    public void getUserpropertiesPath_notNull() throws Exception {
        assertNotNull(testee.getUserpropertiesPath());
    }
}
