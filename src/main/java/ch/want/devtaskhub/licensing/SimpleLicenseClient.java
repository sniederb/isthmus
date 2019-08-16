/*
 * Created on 16.08.2019
 */
package ch.want.devtaskhub.licensing;

import java.util.Observable;

public class SimpleLicenseClient implements LicenseClient {

    @Override
    public void update(final Observable o, final Object arg) {
        // no-op
    }

    @Override
    public boolean checkCurrentLicenseKey() {
        return hasValidLicense();
    }

    @Override
    public boolean hasValidLicense() {
        return true;
    }
}
