/*
 * Created on 20 Jul 2018
 */
package ch.want.devtaskhub.state;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Observable;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import ch.want.devtaskhub.ApplicationRootLocator;

/**
 * {@code application.yml} defines that userproperties will be saved in {@code config/devtaskhub.yml}. That file
 * holds the entire configuration as defined on the Isthmus web interface.
 */
@Component
public class UserPropertiesManager extends Observable {

    private static final Logger LOG = LoggerFactory.getLogger(UserPropertiesManager.class);
    @Autowired
    private UserProperties userProperties;
    @Autowired
    private ApplicationProperties applicationProperties;
    private File propertiesFile;
    private final ObjectMapper mapper;
    private final boolean enableConfigurationEditWatcher;

    public UserPropertiesManager() {
        this(true);
    }

    public UserPropertiesManager(final boolean enableConfigurationEditWatcher) {
        mapper = new ObjectMapper(new YAMLFactory());
        this.enableConfigurationEditWatcher = enableConfigurationEditWatcher;
    }

    @PostConstruct
    public void init() throws IOException {
        initPropertyFileReference();
        readPropertiesFromFile();
        if (enableConfigurationEditWatcher) {
            new Thread(new ConfigurationEditWatcher()).start();
        }
    }

    private void initPropertyFileReference() {
        if (applicationProperties.getUserpropertiesPath().startsWith("classpath:")) {
            propertiesFile = new File(ClassLoader.getSystemResource(applicationProperties.getUserpropertiesPath().substring(10)).getFile());
        } else {
            final File rootDir = new File(new ApplicationRootLocator().getPropertyValue());
            propertiesFile = new File(rootDir, applicationProperties.getUserpropertiesPath());
            if (!propertiesFile.getParentFile().exists()) {
                propertiesFile.getParentFile().mkdirs();
            }
            if (!propertiesFile.exists()) {
                userProperties.setUntouched(true);
                writePropertiesToFile();
            }
        }
    }

    private void readPropertiesFromFile() {
        LOG.debug("Reading configuration from {}", propertiesFile);
        try {
            final UserProperties newProperties = mapper.readValue(propertiesFile, UserProperties.class);
            this.userProperties.copyFrom(newProperties);
            this.onPropertiesChanged();
        } catch (final IOException ex) {
            LOG.warn("Discarding configuration file", ex);
        }
    }

    public void writePropertiesToFile() {
        LOG.debug("Storing configuration to {}", propertiesFile);
        try {
            mapper.writeValue(propertiesFile, userProperties);
        } catch (final IOException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    private void onPropertiesChanged() {
        setChanged();
        notifyObservers();
    }

    private class ConfigurationEditWatcher implements Runnable {

        private final WatchService watcher;
        private final Path configDir;

        ConfigurationEditWatcher() throws IOException {
            watcher = FileSystems.getDefault().newWatchService();
            configDir = Paths.get(propertiesFile.getParentFile().toURI());
            configDir.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY);
        }

        @Override
        public void run() {
            try {
                WatchKey key;
                while ((key = watcher.take()) != null) {
                    processWatchKey(key);
                }
            } catch (final InterruptedException | IOException ex) { // NOSONAR
                LOG.info("Terminating WatchService on configuration file");
            }
        }

        @SuppressWarnings("unchecked")
        private void processWatchKey(final WatchKey key) throws IOException {
            for (final WatchEvent<?> event : key.pollEvents()) {
                if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
                    processWatchEvent((WatchEvent<Path>) event);
                }
            }
            key.reset();
        }

        private void processWatchEvent(final WatchEvent<Path> pathEvent) throws IOException {
            final Path filename = pathEvent.context();
            final Path modifiedFile = configDir.resolve(filename);
            if (Files.isSameFile(modifiedFile, propertiesFile.toPath())) {
                readPropertiesFromFile();
            }
        }
    }
}
