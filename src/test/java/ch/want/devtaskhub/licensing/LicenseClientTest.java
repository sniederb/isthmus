/*
 * Created on 2 Aug 2018
 */
package ch.want.devtaskhub.licensing;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.*;
import static org.junit.jupiter.api.Assertions.*;

import java.net.NetworkInterface;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ch.want.devtaskhub.AbstractSpringConfiguredTest;
import ch.want.devtaskhub.ruleengine.RestClientStub;

public class LicenseClientTest extends AbstractSpringConfiguredTest {

    @Autowired
    private LicenseClient testee;

    @Test
    public void isValidLicense_simple() throws Exception {
        // act
        testee.checkCurrentLicenseKey();
        // arrange
        final String lastUrl = RestClientStub.getLastUrl();
        assertNotNull(lastUrl);
        assertThat(lastUrl, containsString("licenseKey=123456"));
        assertThat(lastUrl, containsString("s="));
        assertThat(lastUrl, containsString("h=9bb5eccdb8b9027f6ded60ede3bed86a39f31080c7f003f97019430242ad1e1d"));
        assertThat(lastUrl, containsString("m="));
    }

    @Test
    public void getInstanceIdentifier_currentInterfaces() throws Exception {
        // act
        final String result = testee.getInstanceIdentifier(NetworkInterface.getNetworkInterfaces());
        // assert
        // as the environment is unknown, lack of exception is all we care about here
        assertNotNull(result);
    }
}
