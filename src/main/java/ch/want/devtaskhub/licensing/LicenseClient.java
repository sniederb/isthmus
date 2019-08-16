/*
 * Created on 18 Jul 2018
 */
package ch.want.devtaskhub.licensing;

import java.util.Observer;

public interface LicenseClient extends Observer {

    boolean checkCurrentLicenseKey();

    boolean hasValidLicense();
}
