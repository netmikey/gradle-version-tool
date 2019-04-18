package io.github.netmikey.gradleversionchecker;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.errors.NoRemoteRepositoryException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.lib.BranchTrackingStatus;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.model.build.BuildEnvironment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.github.netmikey.gradleversionchecker.Main.Action;

/**
 * Does the actual inspection of a found project directory.
 */
@Component
public class ProjectAnalyzer {

    @Autowired
    private GradleInfo gradleInfo;

    @Autowired
    private VersionNumberComparator versionComparator;

    @Value("${action:LIST}")
    private Action action;

    @Value("${dry-run:false}")
    private boolean dryRun;

    /**
     * Main entry method for analyzing the specified project.
     * 
     * @param projectDir
     *            The project's directory.
     * @return The metadata extracted from the project directory.
     */
    public Optional<ProjectMetadata> analyzeProjectDir(File projectDir) {
        // Only process projects that use the wrapper
        if (!new File(projectDir, "gradlew").isFile()) {
            return Optional.empty();
        }

        try {
            ProjectMetadata project = new ProjectMetadata();
            analyzeGradle(projectDir, project);
            analyzeGit(project);
            return Optional.of(project);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private void analyzeGradle(File projectDir, ProjectMetadata project) throws IOException {
        try (ProjectConnection projectConnection = GradleConnector.newConnector().forProjectDirectory(projectDir)
            .connect()) {

            BuildEnvironment environment = projectConnection.model(BuildEnvironment.class).get();

            String gradleVersion = environment.getGradle().getGradleVersion();
            project.setGradleVersion(gradleVersion);
            project.setGradleVersionUpToDate(versionComparator.compare(gradleVersion,
                gradleInfo.latestGradleVersion()) >= 0);
            project.setProjectDir(environment.getBuildIdentifier().getRootDir().getCanonicalFile());
            project.setGradleVersionSameMajorAsCurrent(
                versionComparator.isSameMajor(gradleVersion, gradleInfo.latestGradleVersion()));
        }
    }

    private void analyzeGit(ProjectMetadata project) throws IOException {
        FileRepositoryBuilder gitBuilder = new FileRepositoryBuilder()
            // scan environment GIT_* variables
            .readEnvironment()
            // scan up the file system tree
            .findGitDir(project.getProjectDir());
        boolean isUnderGitVersionControl = gitBuilder.getGitDir() != null;

        project.setUnderGitVersionControl(isUnderGitVersionControl);
        if (isUnderGitVersionControl) {
            try (Repository repository = gitBuilder.build()) {
                project.setBranch(repository.getBranch());

                // Fist, fetch from remote
                if (Action.UPGRADE.equals(action) && !dryRun) {
                    try (Git gitCall = new Git(repository)) {
                        gitCall.fetch().call();
                    } catch (InvalidRemoteException e) {
                        if (e.getCause() instanceof NoRemoteRepositoryException) {
                            // remote 'origin' not present: that's fine
                            project.setGitRemoteTracking(false);
                        } else {
                            throw new RuntimeException("Unable to run 'git fetch': " + e.getMessage(), e);
                        }
                    } catch (NoWorkTreeException | GitAPIException e) {
                        throw new RuntimeException("Unable to run 'git fetch': " + e.getMessage(), e);
                    }
                }

                try (Git gitCall = new Git(repository)) {
                    Status status = gitCall.status().call();
                    project.setClean(status.isClean());
                } catch (NoWorkTreeException | GitAPIException e) {
                    throw new RuntimeException("Unable to run 'git status': " + e.getMessage(), e);
                }

                BranchTrackingStatus trackingStatus = BranchTrackingStatus.of(repository, repository.getBranch());
                if (trackingStatus == null) {
                    project.setGitRemoteTracking(false);
                } else {
                    project.setGitRemoteTracking(true);
                    project.setGitAheadCount(trackingStatus.getAheadCount());
                    project.setGitBehindCount(trackingStatus.getBehindCount());
                }
            }
        }
    }
}
