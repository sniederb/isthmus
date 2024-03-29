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
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

import ch.want.devtaskhub.common.RestClient;
import ch.want.devtaskhub.common.RestClientFactory;
import ch.want.devtaskhub.licensing.LicenseClient;
import ch.want.devtaskhub.licensing.SimpleLicenseClient;
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
    public FreeMarkerConfigurer freeMarkerConfigurer() {
        final FreeMarkerConfigurer configurer = new FreeMarkerConfigurer();
        // prevent auto-configured attempt to load pre-defined templates
        configurer.setTemplateLoaderPaths();
        return configurer;
    }

    @Bean
    public TaskScheduler taskScheduler() {
        return new ConcurrentTaskScheduler(); // single threaded by default
    }

    @Bean
    @Autowired
    public ScheduleEngine scheduleEngine(final UserPropertiesManager userPropertiesManager) {
        final ScheduleEngine scheduleEngine = new ScheduleEngine();
        userPropertiesManager.addObserver(scheduleEngine);
        return scheduleEngine;
    }

    @Profile("!test")
    static class ProductionConfiguration {

        @Bean
        @Autowired
        public UserPropertiesManager userPropertiesManager(final LicenseClient licenseClient) {
            final UserPropertiesManager userPropertiesManager = new UserPropertiesManager(false);
            userPropertiesManager.addObserver(licenseClient);
            return userPropertiesManager;
        }

        @Bean
        public RestClientFactory restClientFactory() {
            return new RestClientFactory(RestClient.class.getName());
        }
        // need to setup a LicenseClient somehow, somewhere
    }

    @Profile({ "test", "workspace" })
    static class TestConfiguration {

        @Bean
        @Autowired
        public UserPropertiesManager userPropertiesManager(final LicenseClient licenseClient) {
            final UserPropertiesManager userPropertiesManager = new UserPropertiesManager(false);
            userPropertiesManager.addObserver(licenseClient);
            return userPropertiesManager;
        }

        @Bean
        public RestClientFactory restClientFactory() {
            return new RestClientFactory("ch.want.devtaskhub.ruleengine.RestClientStub");
        }

        @Bean
        public LicenseClient licenseClient() {
            return new SimpleLicenseClient();
        }
    }
}
