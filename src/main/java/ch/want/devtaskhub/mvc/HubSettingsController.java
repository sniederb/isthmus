/*
 * Created on 13 Jul 2018
 */
package ch.want.devtaskhub.mvc;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import ch.want.devtaskhub.licensing.LicenseClient;
import ch.want.devtaskhub.ruleengine.ScheduleEngine;
import ch.want.devtaskhub.state.ApplicationState;
import ch.want.devtaskhub.state.UserProperties;
import ch.want.devtaskhub.state.UserPropertiesManager;

@RestController
public class HubSettingsController {

    @Autowired
    private UserProperties userProperties;
    @Autowired
    private ApplicationState applicationState;
    @Autowired
    private UserPropertiesManager userPropertiesManager;
    @Autowired
    private ScheduleEngine scheduleEngine;
    @Autowired
    private LicenseClient licenseClient;

    @RequestMapping(value = "/settings", method = { RequestMethod.GET }, produces = MediaType.APPLICATION_JSON_VALUE)
    public UserProperties getSettings() {
        return userProperties;
    }

    @RequestMapping(value = "/settings", method = { RequestMethod.PUT })
    public Map<String, Object> saveSettings(@RequestBody final UserProperties properties) {
        properties.validate();
        userProperties.copyFrom(properties);
        userPropertiesManager.writePropertiesToFile();
        final Map<String, Object> response = new HashMap<>();
        response.put("message", "Ok");
        return response;
    }

    @RequestMapping(value = "/console", method = { RequestMethod.GET })
    public Map<String, Object> console() {
        final Map<String, Object> response = new HashMap<>();
        response.put("console", this.applicationState.getLastActions());
        return response;
    }

    @RequestMapping(value = "/license", method = { RequestMethod.GET })
    public Map<String, Object> getLicenseStatus() throws IOException {
        final Map<String, Object> response = new HashMap<>();
        if (licenseClient.checkCurrentLicenseKey()) {
            response.put("message", "Ok");
            return response;
        }
        throw new IllegalArgumentException("Invalid license");
    }

    @RequestMapping(value = "/triggercron/{ruleIndex}", method = { RequestMethod.POST })
    public Map<String, Object> triggerCronRule(@PathVariable final int ruleIndex) {
        applicationState.addAction("Forcing start of scheduled rule index " + ruleIndex);
        scheduleEngine.forceRun(ruleIndex);
        return new HashMap<>();
    }
}
