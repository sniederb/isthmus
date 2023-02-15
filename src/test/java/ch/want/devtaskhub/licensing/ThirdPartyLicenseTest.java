package ch.want.devtaskhub.licensing;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.commons.codec.Charsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.util.StreamUtils;

/**
 * The copyleft is an invented term, used to describe a copyright that requires anyone distributing a copy or derived copy to allow
 * redistribution of their code.
 * Weak copyleft means derived work need not have the same license. Strong copyleft does.
 * <dt>Apache License</dt>
 * <dd>Permissive license. Modify and distribution are allowed. Commercial use is allowed. Liability is excluded</dd>
 * <dt>GNU GPL</dt>
 * <dd>Strong copyleft license, ie. derived work must be under the same license. Any modifications to or software including
 * (via compiler) GPL-licensed code must also be made available under the GPL along with build & install instructions.</dd>
 * <dt>Eclipse Public License</dt>
 * <dd>Similar to GPL, but weak copyleft license</dd>
 * <dt>LGPL</dt>
 * <dd>Strong copyleft license. Derivatives works (including modifications or anything statically linked to the library) can
 * only be redistributed under LGPL, but applications that use the library don't have to be.</dd>
 * <dt>MIT License</dt>
 * <dd>Modify and distribution are allowed. Commercial use is allowed. Liability is excluded</dd>
 * <dt>3-Clause BSD License</dt>
 * <dd>Modify and distribution are allowed. Commercial use is allowed. License and copyright information must be retained</dd>
 *
 * @see https://tldrlegal.com/
 */
class ThirdPartyLicenseTest {

    @Test
    void thirdPartyLicenses_commercialAllowed() throws Exception {
        final Map<String, String> licenses = getLicensePerArtefact();
        // assert
        assertNotNull(licenses);
        final Stream<Executable> executables = licenses.entrySet().stream()//
                .map(entry -> () -> {
                    assertFalse(entry.getValue().contains("non-commercial"),
                            "Problematic license [" + entry.getValue() + "] for artifact [" + entry.getKey() + "]");
                });
        assertAll(executables);
    }

    private Map<String, String> getLicensePerArtefact() throws IOException {
        final Map<String, String> result = new HashMap<>();
        final Pattern regExpLicenseName = Pattern.compile("\\s*<name>([^<]+)</name>.*");
        final Pattern regExpArtifactId = Pattern.compile("\\s*<artifactId>([^<]+)</artifactId>.*");
        final String[] lines = StreamUtils.copyToString(this.getClass().getResourceAsStream("/licenses/licenses.xml"), Charsets.UTF_8)
                .split("[\\r\\n]+");
        String currentArtefact = null;
        for (final String line : lines) {
            Matcher matcher = regExpArtifactId.matcher(line);
            if (matcher.find()) {
                currentArtefact = matcher.group(1);
            } else {
                matcher = regExpLicenseName.matcher(line);
                if (matcher.find()) {
                    result.put(currentArtefact, matcher.group(1));
                }
            }
        }
        return result;
    }
}
