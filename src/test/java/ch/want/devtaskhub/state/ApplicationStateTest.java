package ch.want.devtaskhub.state;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ch.want.devtaskhub.AbstractSpringConfiguredTest;

public class ApplicationStateTest extends AbstractSpringConfiguredTest {

    @Autowired
    private ApplicationState testee;

    @Test
    public void actionQueue_notNull() throws Exception {
        for (int i = 0; i < 100; i++) {
            testee.addAction("junit-" + i);
        }
        final List<String> actions = testee.getLastActions();
        assertEquals(ApplicationState.ACTION_BUFFER_SIZE, actions.size());
    }
}
