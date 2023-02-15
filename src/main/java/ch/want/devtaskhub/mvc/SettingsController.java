package ch.want.devtaskhub.mvc;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import ch.want.devtaskhub.licensing.LicenseClient;
import ch.want.devtaskhub.ruleengine.ScheduleEngine;
import ch.want.devtaskhub.state.ApplicationState;
import ch.want.devtaskhub.state.UserProperties;
import ch.want.devtaskhub.state.UserPropertiesManager;

@RestController
public class SettingsController {

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

    @GetMapping("/settings")
    public UserProperties getSettings() {
        return userProperties;
    }

    @PutMapping("/settings")
    public Map<String, Object> saveSettings(@RequestBody final UserProperties properties) {
        properties.validate();
        userProperties.copyFrom(properties);
        userPropertiesManager.writePropertiesToFile();
        final Map<String, Object> response = new HashMap<>();
        response.put("message", "Ok");
        return response;
    }

    @GetMapping("/console")
    public Map<String, Object> console() {
        final Map<String, Object> response = new HashMap<>();
        response.put("console", this.applicationState.getLastActions());
        return response;
    }

    @GetMapping("/license")
    public Map<String, Object> getLicenseStatus() {
        final Map<String, Object> response = new HashMap<>();
        if (licenseClient.checkCurrentLicenseKey()) {
            response.put("message", "Ok");
            return response;
        }
        throw new IllegalArgumentException("Invalid license");
    }

    @PostMapping("/triggercron/{ruleIndex}")
    public Map<String, Object> triggerCronRule(@PathVariable final int ruleIndex) {
        applicationState.addAction("Forcing start of scheduled rule index " + ruleIndex);
        scheduleEngine.forceRun(ruleIndex);
        return new HashMap<>();
    }
}
