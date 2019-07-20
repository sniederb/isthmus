package ch.want.devtaskhub.state;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/**
 * Read classpath:application.yml, which holds the internal application configuration
 */
@Component
@ConfigurationProperties(prefix = "internal")
@Configuration
public class ApplicationProperties {

    private static final Logger LOG = LoggerFactory.getLogger(ApplicationProperties.class);
    private String userpropertiesPath;

    @PostConstruct
    public void logSettings() {
        LOG.info("userpropertiesPath={}", userpropertiesPath);
    }

    public String getUserpropertiesPath() {
        return userpropertiesPath;
    }

    public void setUserpropertiesPath(final String userpropertiesPath) {
        this.userpropertiesPath = userpropertiesPath;
    }
}