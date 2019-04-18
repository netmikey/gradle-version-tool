package io.github.netmikey.gradleversionchecker;

import static org.fusesource.jansi.Ansi.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.events.OperationType;
import org.gradle.tooling.events.ProgressEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

/**
 * Encapsulates the logic of the upgrade action.
 */
@Component
public class UpgradeAction implements Consumer<ProjectMetadata> {

    @Autowired
    private ConsoleWriter out;

    @Autowired
    private GradleInfo gradleInfo;

    @Value("${major-upgrades:false}")
    private boolean majorUpgrades;

    @Value("${dry-run:false}")
    private boolean dryRun;

    @Value("${check-tasks:clean,build,assemble}")
    private String[] checkTasks;

    @Value("${commit:true}")
    private boolean commit;

    @Value("${push:${commit:true}}")
    private boolean push;

    private boolean windows = System.getProperty("os.name").toLowerCase().indexOf("win") >= 0;

    @Override
    public void accept(ProjectMetadata project) {
        out.println(">> Project: " + project.getProjectDir().getPath());
        out.println(ansi().a("    Gradle: ").bold().format("%-10s ", project.getGradleVersion()).boldOff()
            .a(out.formatVersionUpToDate(project)));

        if (project.isUnderGitVersionControl()) {
            out.println(ansi().a("    Branch: ").format("%-10s ", project.getBranch()));
        }

        boolean doUpgrade = evaluateUpgradability(project);
        if (doUpgrade) {
            out.println(
                ansi().fgBrightGreen().a("   Upgrading project to Gradle " + gradleInfo.latestGradleVersion()).reset());

            if (!dryRun) {
                doUpgrate(project);
                try {
                    if (project.isUnderGitVersionControl()) {
                        doCommitAndPush(project);
                    } else {
                        out.println("   Project not under version control");
                    }
                } catch (RuntimeException e) {
                    out.println(ansi().fgBrightRed().a("     Commit/Push error: " + e.getMessage()).reset());
                }
            }
        }

        out.println("");
    }

    private void doCommitAndPush(ProjectMetadata project) {
        if (!commit) {
            out.println("   Committing disabled");
            return;
        }

        out.println("   Committing changes");

        FileRepositoryBuilder gitBuilder = new FileRepositoryBuilder()
            // scan environment GIT_* variables
            .readEnvironment()
            // scan up the file system tree
            .findGitDir(project.getProjectDir());

        out.printVolatile(">> Adding changes to Git index...");
        try (Repository repository = gitBuilder.build()) {
            try (Git gitCall = new Git(repository)) {
                gitCall.add().addFilepattern(".").call();
            }
            out.printVolatile(">> Committing...");
            try (Git gitCall = new Git(repository)) {
                String message = "build: upgrade Gradle from v" + project.getGradleVersion() + " to v"
                    + gradleInfo.latestGradleVersion();
                gitCall.commit().setAllowEmpty(false).setMessage(message).call();
            }
            out.println("    Commit successful");
            if (push) {
                out.printVolatile(">> Pushing...");
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try (Git gitCall = new Git(repository)) {
                    gitCall.push().setAtomic(true).setOutputStream(baos).call();
                } catch (RuntimeException | GitAPIException e) {
                    throw new RuntimeException(baos + "\n" + e.getMessage(), e);
                }
                out.println("    Push successful");
            }
        } catch (IOException | GitAPIException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private void doUpgrate(ProjectMetadata project) {
        try (ProjectConnection projectConnection = GradleConnector.newConnector()
            .forProjectDirectory(project.getProjectDir()).connect()) {

            // Make sure the build works before attempting the upgrade
            try {
                out.printVolatile(">> Building before upgrading...");
                projectConnection.newBuild().forTasks(checkTasks)
                    .addProgressListener(e -> reportProgress(">> Pre-upgrade build: ", e), OperationType.TASK,
                        OperationType.TEST)
                    .run();
                out.println("    Pre-upgrade build successful");
            } catch (Exception e) {
                throw new UpgradeException("Pre-upgrade build failed: " + e.getMessage(), e);
            }

            // Upgrade the wrapper
            try {
                out.printVolatile(">> Upgrading the Gradle wrapper...");
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                String wrapperScript = new File(project.getProjectDir(), windows ? "gradlew.bat" : "gradlew")
                    .getCanonicalPath();
                ProcessBuilder pb = new ProcessBuilder(wrapperScript, "wrapper", "--gradle-version",
                    gradleInfo.latestGradleVersion())
                        .directory(project.getProjectDir());
                Process process = pb.start();
                StreamUtils.copy(process.getInputStream(), baos);
                int exitValue = process.waitFor();
                if (exitValue != 0 && baos.toString().indexOf("BUILD SUCCESSFUL") < 0) {
                    throw new UpgradeException(
                        "Process returned non-zero value (" + exitValue + "). Output: " + baos.toString());
                }
                out.println("    Wrapper upgrade successful");
            } catch (Exception e) {
                throw new UpgradeException("Wrapper upgrade failed: " + e.getMessage(), e);
            }
        }

        // Now that the gradle version has changed, create a new connection
        try (ProjectConnection projectConnection = GradleConnector.newConnector()
            .forProjectDirectory(project.getProjectDir()).connect()) {

            // Make sure the build still works after the upgrade
            try {
                out.printVolatile(">> Building after upgrading...");
                projectConnection.newBuild().forTasks(checkTasks)
                    .addProgressListener(e -> reportProgress(">> Post-upgrade build: ", e), OperationType.TASK,
                        OperationType.TEST)
                    .run();
                out.println("    Post-upgrade build successful");
            } catch (Exception e) {
                throw new UpgradeException("Post-upgrade build failed: " + e.getMessage(), e);
            }
        }
    }

    private void reportProgress(String prefix, ProgressEvent event) {
        out.printVolatile(prefix + event.getDisplayName());
    }

    private boolean evaluateUpgradability(ProjectMetadata project) {
        if (project.isGradleVersionUpToDate()) {
            return false;
        }
        boolean doUpgrade = true;
        if (project.isUnderGitVersionControl()) {
            if (!project.isClean()) {
                notUpgrading(doUpgrade, "Git working directory not clean");
                doUpgrade = false;
            }
            if (!project.isInSyncWithRemote()) {
                notUpgrading(doUpgrade, "Branch " + project.getBranch() + " not in sync with remote: "
                    + project.getGitAheadCount() + " ahead, " + project.getGitBehindCount() + " behind");
                doUpgrade = false;
            }
        }
        if (!majorUpgrades && !project.isGradleVersionSameMajorAsCurrent()) {
            notUpgrading(doUpgrade, "not the same major version and major upgrades disabled");
            doUpgrade = false;
        }
        return doUpgrade;
    }

    private void notUpgrading(boolean firstTimeNegation, String reason) {
        if (firstTimeNegation) {
            out.println(
                ansi().fgBrightRed().a("   Not upgrading to Gradle " + gradleInfo.latestGradleVersion() + ":").reset());
        }
        out.println("    - " + reason);
    }
}
