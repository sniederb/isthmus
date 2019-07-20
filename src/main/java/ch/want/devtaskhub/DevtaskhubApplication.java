package ch.want.devtaskhub;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.web.client.RestTemplate;

import ch.want.devtaskhub.common.RestClient;
import ch.want.devtaskhub.common.RestClientFactory;
import ch.want.devtaskhub.licensing.LicenseClient;
import ch.want.devtaskhub.ruleengine.ScheduleEngine;
import ch.want.devtaskhub.state.UserPropertiesManager;

/**
 *
 */
@SpringBootApplication
@EnableAutoConfiguration
@EnableScheduling
@ComponentScan(basePackages = { //
        "ch.want.devtaskhub", //
        "ch.want.devtaskhub.mvc", //
        "ch.want.devtaskhub.ruleengine", //
        "ch.want.devtaskhub.licensing"
})
public class DevtaskhubApplication {

    public static void main(final String[] args) {
        SpringApplication.run(DevtaskhubApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate(final RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder
                .setConnectTimeout(20 * 1000)
                .setReadTimeout(20 * 1000)
                .build();
    }

    @Bean
    public TaskScheduler taskScheduler() {
        return new ConcurrentTaskScheduler(); // single threaded by default
    }

    @Bean
    @Profile("!test")
    public UserPropertiesManager userPropertiesManager() {
        return new UserPropertiesManager();
    }

    @Bean
    @Profile("test")
    public UserPropertiesManager testUserPropertiesManager() {
        return new UserPropertiesManager(false);
    }

    @Bean
    @Profile("!test")
    @Autowired
    public LicenseClient licenseClient(final UserPropertiesManager userPropertiesManager) {
        final LicenseClient licenseClient = new LicenseClient();
        userPropertiesManager.addObserver(licenseClient);
        return licenseClient;
    }

    @Bean
    @Profile("test")
    @Autowired
    public LicenseClient testLicenseClient(final UserPropertiesManager userPropertiesManager) {
        final LicenseClient licenseClient = new LicenseClient(false);
        userPropertiesManager.addObserver(licenseClient);
        return licenseClient;
    }

    @Bean
    @Autowired
    public ScheduleEngine scheduleEngine(final UserPropertiesManager userPropertiesManager) {
        final ScheduleEngine scheduleEngine = new ScheduleEngine();
        userPropertiesManager.addObserver(scheduleEngine);
        return scheduleEngine;
    }

    @Bean
    @Profile("!test")
    public RestClientFactory productiveRestClientFactory() {
        return new RestClientFactory(RestClient.class.getName());
    }

    @Bean
    @Profile("test")
    public RestClientFactory testRestClientFactory() {
        return new RestClientFactory("ch.want.devtaskhub.ruleengine.RestClientStub");
    }
}
