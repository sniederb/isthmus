/*
 * Created on 13 Jul 2018
 */
package ch.want.devtaskhub.mvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.nio.charset.Charset;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.util.StreamUtils;

import ch.want.devtaskhub.AbstractSpringConfiguredTest;
import ch.want.devtaskhub.state.ApplicationState;
import ch.want.devtaskhub.state.UserProperties;
import ch.want.devtaskhub.state.UserPropertiesManager;

public class HubSettingsControllerTest extends AbstractSpringConfiguredTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ApplicationState applicationState;
    @Autowired
    private UserProperties userProperties;
    @Autowired
    private UserPropertiesManager userPropertiesManager;
    private UserProperties userPropertiesBackup;

    @BeforeEach
    public void rememberSettings() {
        userPropertiesBackup = new UserProperties();
        userProperties.copyTo(userPropertiesBackup);
    }

    @AfterEach
    public void resetSettings() {
        userProperties.copyFrom(userPropertiesBackup);
        userPropertiesManager.writePropertiesToFile();
    }

    @Test
    public void getSettings_noAuthentication() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/settings")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());
    }

    @Test
    public void getSettings() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/settings")
                .with(httpBasic(userProperties.getUsername(), userProperties.getPassword()))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("licenseKey")));
    }

    @Test
    public void saveSettings() throws Exception {
        final String payload = StreamUtils.copyToString(this.getClass().getResourceAsStream("/settings/settings-request-from-webclient.json"),
                Charset.forName("UTF-8"));
        mvc.perform(MockMvcRequestBuilders.put("/settings")
                .with(httpBasic(userProperties.getUsername(), userProperties.getPassword()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void license() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/license")
                .with(httpBasic(userProperties.getUsername(), userProperties.getPassword()))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ok")));
    }

    @Test
    public void console() throws Exception {
        applicationState.addAction("Running JUnit tests");
        mvc.perform(MockMvcRequestBuilders.get("/console")
                .with(httpBasic(userProperties.getUsername(), userProperties.getPassword()))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Running JUnit tests")));
    }
}
