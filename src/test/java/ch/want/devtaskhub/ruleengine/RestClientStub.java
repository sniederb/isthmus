/*
 * Created on 27 Jul 2018
 */
package ch.want.devtaskhub.ruleengine;

import java.io.IOException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.want.devtaskhub.common.RestClient;
import ch.want.devtaskhub.state.Endpoint;

public class RestClientStub extends RestClient {

    private static final Logger LOG = LoggerFactory.getLogger(RestClientStub.class);
    private static HttpEntity<?> lastBody;
    private static String lastUrl;
    private static boolean assumeLicenseValid = true;

    public RestClientStub(final Endpoint endpoint) {
        super(endpoint);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected synchronized <T> Optional<T> exchange(final String urlQuery, final HttpMethod method, final HttpEntity<?> body, final Class<T> responseClass) {
        lastUrl = getEndpoint(urlQuery);
        LOG.info("Got {} request pointing to ", method, lastUrl);
        lastBody = body;
        if (urlQuery.indexOf("licenseKey") >= 0) {
            return (Optional<T>) licenseResponse();
        }
        return Optional.empty();
    }

    private Optional<JsonNode> licenseResponse() {
        try {
            return Optional.of(new ObjectMapper().readTree("{\"valid\":" + assumeLicenseValid + "}"));
        } catch (final IOException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    public static void resetLastExchange() {
        lastBody = null;
        lastUrl = null;
    }

    public static HttpEntity<?> getLastExchangeBody() {
        return lastBody;
    }

    public static String getLastUrl() {
        return lastUrl;
    }

    public static void assumeLicenseValid(final boolean valid) {
        assumeLicenseValid = valid;
    }
}
