package ch.want.devtaskhub.common;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.want.devtaskhub.state.Endpoint;

public class RestClientFactory {

    private static final Logger LOG = LoggerFactory.getLogger(RestClientFactory.class);
    private final Constructor<?> restClientConstructor;

    public RestClientFactory(final String clientClassname) {
        Constructor<?> ctor = null;
        try {
            final Class<?> restClientClass = Class.forName(clientClassname);
            ctor = restClientClass.getConstructor(Endpoint.class);
        } catch (NoSuchMethodException | SecurityException | ClassNotFoundException e) {
            LOG.error("Unable to build c'tor for class {}", clientClassname, e);
        }
        restClientConstructor = ctor;
    }

    public RestClient getClient(final Endpoint endpoint) {
        if (restClientConstructor == null) {
            throw new IllegalStateException("Undefined constructor");
        }
        try {
            return (RestClient) restClientConstructor.newInstance(endpoint);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new IllegalStateException("Unable to create REST client", e);
        }
    }
}
