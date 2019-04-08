package io.github.netmikey.gradleversionchecker;

import static org.fusesource.jansi.Ansi.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import org.fusesource.jansi.Ansi;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.model.build.BuildEnvironment;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Does the actual inspection of a found project directory.
 */
public class ProjectAnalyzer {

    private ConsoleWriter out;

    private int numFound = 0;

    private Optional<String> currentGradleVersion = Optional.empty();

    private VersionNumberComparator versionComparator = new VersionNumberComparator();

    /**
     * Default constructor.
     * 
     * @param writer
     *            The console writer to be used.
     */
    public ProjectAnalyzer(ConsoleWriter writer) {
        this.out = writer;
        init();
    }

    @SuppressWarnings("unchecked")
    private void init() {
        try {
            URL url = new URL("https://api.github.com/repos/gradle/gradle/releases");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            ObjectMapper mapper = new ObjectMapper();
            List<Map<String, Object>> ghData;
            try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                ghData = mapper.readValue(in, List.class);
            }
            Pattern releaseVersionPattern = Pattern.compile("[\\d\\.]+");
            currentGradleVersion = ghData.stream()
                .map(version -> version.get("name").toString())
                .filter(name -> releaseVersionPattern.matcher(name).matches())
                .sorted(versionComparator)
                .reduce((first, second) -> second);
        } catch (IOException e) {
            out.println("WARN: unable to load the current gradle version: " + e.getMessage());
        }
    }

    /**
     * Main entry method for analyzing the specified project.
     * 
     * @param projectDir
     *            The project's directory.
     */
    public void analyzeProjectDir(File projectDir) {
        // Only process projects that use the wrapper
        if (!new File(projectDir, "gradlew").isFile()) {
            return;
        }

        numFound++;

        try (ProjectConnection projectConnection = GradleConnector.newConnector().forProjectDirectory(projectDir)
            .connect()) {

            BuildEnvironment environment = projectConnection.model(BuildEnvironment.class).get();
            String gradleVersion = environment.getGradle().getGradleVersion();
            Ansi gradleVersionUpToDate = ansi();
            if (currentGradleVersion.isPresent()) {
                if (versionComparator.compare(gradleVersion, currentGradleVersion.get()) < 0) {
                    gradleVersionUpToDate = ansi().fgBrightRed().a("NOT UP TO DATE").reset();
                } else {
                    gradleVersionUpToDate = ansi().fgGreen().a("UP TO DATE").reset();
                }
            }
            out.println(">> Project: " + environment.getBuildIdentifier().getRootDir().getCanonicalPath());
            out.println(ansi().a("    Gradle: ").bold().format("%-10s", gradleVersion).boldOff().a("   ")
                .a(gradleVersionUpToDate));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get the number of found gradle projects.
     * 
     * @return Returns the numer.
     */
    public int getNumFound() {
        return numFound;
    }

    private static class VersionNumberComparator implements Comparator<String> {
        @Override
        public int compare(String version1, String version2) {
            String[] arr1 = version1.split("\\.");
            String[] arr2 = version2.split("\\.");

            int maxLength = Math.max(arr1.length, arr2.length);

            for (int i = 0; i < maxLength; i++) {
                if (i >= arr1.length || Integer.parseInt(arr1[i]) < Integer.parseInt(arr2[i])) {
                    return -1;
                }
                if (i >= arr2.length || Integer.parseInt(arr1[i]) > Integer.parseInt(arr2[i])) {
                    return 1;
                }
            }
            return 0;
        }
    }
}
