package io.github.netmikey.gradleversionchecker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Gathers general information about Gradle.
 */
@Component
public class GradleInfo {

    @Autowired
    private VersionNumberComparator versionComparator;

    private String latestGradleVersion;

    /**
     * Returns the latest Gradle release version.
     * 
     * @return The latest Gradle version.
     */
    @SuppressWarnings("unchecked")
    public synchronized String latestGradleVersion() {
        if (latestGradleVersion == null) {
            try {
                URL url = new URL("https://raw.githubusercontent.com/gradle/gradle/master/released-versions.json");
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");

                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> ghData;
                try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                    ghData = mapper.readValue(in, Map.class);
                }
                Pattern releaseVersionPattern = Pattern.compile("[\\d\\.]+");
                latestGradleVersion = ((List<Map<String, String>>) ghData.get("finalReleases")).stream()
                    .map(version -> version.get("version").toString())
                    .filter(name -> releaseVersionPattern.matcher(name).matches())
                    .sorted(versionComparator)
                    .reduce((first, second) -> second)
                    .orElseThrow(() -> new RuntimeException("ERROR: unable to retrieve latest Gradle version"));
            } catch (IOException e) {
                throw new RuntimeException("ERROR: unable to load Gradle version information: " + e.getMessage());
            }
        }
        return latestGradleVersion;
    }
}
