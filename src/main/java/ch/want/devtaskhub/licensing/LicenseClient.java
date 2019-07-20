/*
 * Created on 18 Jul 2018
 */
package ch.want.devtaskhub.licensing;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.collections4.EnumerationUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

import com.fasterxml.jackson.databind.JsonNode;

import ch.want.devtaskhub.common.RestClient;
import ch.want.devtaskhub.common.RestClientFactory;
import ch.want.devtaskhub.state.Endpoint;
import ch.want.devtaskhub.state.UserProperties;

/**
 * See https://stripe.com/docs/testing for Stripe testing
 */
public class LicenseClient implements Observer {

    private static final Logger LOG = LoggerFactory.getLogger(LicenseClient.class);
    private LicenseCheckResult lastCheckResult;
    @Autowired
    protected RestClientFactory restClientFactory;
    @Autowired
    protected UserProperties userProperties;
    @Autowired
    private TaskScheduler executor;
    private final CheckLicenseRunner licenseRunner;
    private final boolean checkAsynchronously;
    private boolean grabLicenseOnNextCheck = false;

    public LicenseClient() {
        this(true);
    }

    public LicenseClient(final boolean checkAsynchronously) {
        licenseRunner = new CheckLicenseRunner();
        this.checkAsynchronously = checkAsynchronously;
    }

    @PostConstruct
    public void scheduleRegularLicenseCheck() {
        executor.schedule(licenseRunner, new CronTrigger("0 15 0/6 ? * *"));
        this.grabLicenseOnNextCheck = true;
        runLicenseCheckNow();
    }

    public boolean checkCurrentLicenseKey() {
        if ((this.lastCheckResult == null) || //
                !this.lastCheckResult.key.equals(getLicenseCheckKey()) || //
                this.lastCheckResult.isExpired()) {
            //
            this.lastCheckResult = null;
            try {
                executeLicenseCheck();
            } catch (final IOException e) {
                LOG.warn(e.getMessage(), e);
            }
        }
        return hasValidLicense();
    }

    private String getLicenseCheckKey() {
        return userProperties.getEmail() + ";" + userProperties.getLicenseKey();
    }

    String getInstanceIdentifier(final Enumeration<NetworkInterface> networkInterfacesEnumeration) {
        try {
            final List<NetworkInterface> networks = EnumerationUtils.toList(networkInterfacesEnumeration);
            final Optional<NetworkInterface> firstNonVirtual = networks.stream()
                    .filter(LicenseClient::isNotLoobackInterface)
                    .filter(LicenseClient::isUp)
                    .filter(LicenseClient::hasHardwareAddress)
                    .findFirst();
            if (firstNonVirtual.isPresent()) {
                return byteArrayToHex(firstNonVirtual.get().getHardwareAddress());
            }
        } catch (final Exception e) { // NOSONAR
            LOG.warn(e.getMessage());
        }
        // if we get here, it's very unlikely a GET to the license server will succeed
        return Integer.toString(hashCode());
    }

    private static boolean isNotLoobackInterface(final NetworkInterface intf) {
        try {
            return !intf.isLoopback();
        } catch (final SocketException e) { // NOSONAR
            return false;
        }
    }

    private static boolean isUp(final NetworkInterface intf) {
        try {
            return intf.isUp();
        } catch (final SocketException e) { // NOSONAR
            return false;
        }
    }

    private static boolean hasHardwareAddress(final NetworkInterface intf) {
        try {
            return intf.getHardwareAddress() != null;
        } catch (final SocketException e) { // NOSONAR
            return false;
        }
    }

    private String byteArrayToHex(final byte[] byteArray) {
        final StringBuilder sb = new StringBuilder(18);
        for (final byte b : byteArray) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * As a license check could incur a charge, make sure that no two checks
     * ever run in parallel
     *
     * @throws IOException
     */
    private synchronized void executeLicenseCheck() throws IOException {
        if (StringUtils.isBlank(userProperties.getEmail())) {
            this.lastCheckResult = new LicenseCheckResult("", false);
            return;
        }
        final Endpoint licensingEndpoint = getVerificationEndpoint();
        if (!hasIpAddressMatch(licensingEndpoint, "62.146.70.231")) {
            this.lastCheckResult = new LicenseCheckResult(getLicenseCheckKey(), false);
            return;
        }
        final RestClient client = restClientFactory.getClient(licensingEndpoint);
        final Map<String, String> parameters = new HashMap<>();
        parameters.put("licenseKey", userProperties.getLicenseKey());
        parameters.put("email", userProperties.getEmail());
        parameters.put("m", getInstanceIdentifier(NetworkInterface.getNetworkInterfaces()));
        parameters.put("h", getHash(userProperties.getLicenseKey()));
        parameters.put("s", Boolean.toString(grabLicenseOnNextCheck));
        final boolean isValidLicense = client.get(parameters, JsonNode.class)
                .map(node -> node.get("valid").asBoolean())
                .orElse(false);
        this.lastCheckResult = new LicenseCheckResult(getLicenseCheckKey(), isValidLicense);
    }

    @Override
    public void update(final Observable o, final Object arg) {
        this.grabLicenseOnNextCheck = true;
        runLicenseCheckNow();
    }

    private void runLicenseCheckNow() {
        if (checkAsynchronously) {
            new Thread(licenseRunner).start();
        } else {
            licenseRunner.run();
        }
    }

    public boolean hasValidLicense() {
        if ((this.lastCheckResult == null) || this.lastCheckResult.isExpired()) {
            // no server response since start, or not since a too long time ... assume false
            return false;
        }
        return this.lastCheckResult.valid;
    }

    private Endpoint getVerificationEndpoint() {
        return new Endpoint("https://isthmus.want.ch/license.php", HttpMethod.GET.toString(), null, null);
    }

    private boolean hasIpAddressMatch(final Endpoint endpoint, final String expectedHostaddress) {
        try {
            final InetAddress address = InetAddress.getByName(new URL(endpoint.getUrl()).getHost());
            return expectedHostaddress.equals(address.getHostAddress());
        } catch (UnknownHostException | MalformedURLException e) { // NOSONAR
            LOG.debug(e.getMessage());
            return false;
        }
    }

    private String getHash(final String key) {
        try {
            final Mac hmacSha256 = Mac.getInstance("HmacSHA256");
            final SecretKeySpec secretKey = new SecretKeySpec("sd234rw432rf3678j7f24da243f554g6".getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmacSha256.init(secretKey);
            return Hex.encodeHexString(hmacSha256.doFinal(key.getBytes(StandardCharsets.UTF_8)));
        } catch (final NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    private class CheckLicenseRunner implements Runnable {

        @Override
        public void run() {
            final int retryCount = 0;
            lastCheckResult = null;
            while (retryCount < 3 && lastCheckResult == null) {
                updateLicenseState();
                if (retryCount < 3 && lastCheckResult == null) {
                    waitBeforeRetry();
                }
            }
        }

        private void updateLicenseState() {
            try {
                executeLicenseCheck();
                grabLicenseOnNextCheck = false;
            } catch (final IOException e) {// NOSONAR
                LOG.debug(e.getMessage());
            }
        }

        private void waitBeforeRetry() {
            try {
                Thread.sleep(60 * 1000L);
            } catch (final InterruptedException e) { // NOSONAR
                // no-op, just continue;
            }
        }
    }

    private static class LicenseCheckResult {

        private final String key;
        private final LocalDateTime lastSuccessfulCheck;
        private final boolean valid;

        LicenseCheckResult(final String key, final boolean valid) {
            this.key = key;
            this.valid = valid;
            this.lastSuccessfulCheck = LocalDateTime.now();
        }

        boolean isExpired() {
            return LocalDateTime.now().minusHours(24).isAfter(this.lastSuccessfulCheck);
        }
    }
}
