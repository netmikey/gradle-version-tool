package io.github.netmikey.gradleversionchecker;

import static org.fusesource.jansi.Ansi.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.fusesource.jansi.Ansi;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.model.build.BuildEnvironment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Does the actual inspection of a found project directory.
 */
@Component
public class ProjectAnalyzer {

    @Autowired
    private ConsoleWriter out;

    @Autowired
    private VersionNumberComparator versionComparator;

    private int numFound = 0;

    private Optional<String> currentGradleVersion = Optional.empty();

    @PostConstruct
    @SuppressWarnings("unchecked")
    private void init() {
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
            currentGradleVersion = ((List<Map<String, String>>) ghData.get("finalReleases")).stream()
                .map(version -> version.get("version").toString())
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

            FileRepositoryBuilder gitBuilder = new FileRepositoryBuilder()
                .readEnvironment() // scan environment GIT_* variables
                .findGitDir(projectDir); // scan up the file system tree
            boolean isUnderGitVersionControl = gitBuilder.getGitDir() != null;

            String branch = null;
            boolean clean = false;
            if (isUnderGitVersionControl) {
                try (Repository repository = gitBuilder.build()) {
                    branch = repository.getBranch();
                    try (Git gitCall = new Git(repository)) {
                        Status status = gitCall.status().call();
                        clean = status.isClean();
                    } catch (NoWorkTreeException | GitAPIException e) {
                        throw new RuntimeException("Unable to run 'git status': " + e.getMessage(), e);
                    }
                }
            }

            out.println(">> Project: " + environment.getBuildIdentifier().getRootDir().getCanonicalPath());
            out.println(ansi().a("    Gradle: ").bold().format("%-10s ", gradleVersion).boldOff()
                .a(gradleVersionUpToDate));
            if (isUnderGitVersionControl) {
                out.println(ansi().a("    Branch: ").format("%-10s ", branch).a(clean ? "working directory clean" : "working directory not clean"));
            }
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
}
