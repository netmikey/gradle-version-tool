package io.github.netmikey.gradleversionchecker;

import java.io.File;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Scans directories for Gradle projects.
 */
@Component
public class Scanner {

    @Autowired
    private ConsoleWriter out;

    @Autowired
    private ProjectAnalyzer analyzer;

    /**
     * Scan from the specified baseDir.
     * 
     * @param baseDir
     *            The directory to start with.
     * @param callback
     *            A callback that will get called for any found and analyzed
     *            project directory.
     */
    public void scan(File baseDir, Consumer<ProjectMetadata> callback) {
        out.println("Scanning for Gradle projects in " + baseDir.getAbsolutePath() + " ...\n");
        scanChildren(baseDir, callback);
    }

    private void scanChildren(File baseDir, Consumer<ProjectMetadata> callback) {
        out.printVolatile(">> Scanning: " + baseDir.getAbsolutePath());

        if (!baseDir.isDirectory()) {
            return;
        }

        File buildFile = new File(baseDir, "build.gradle");

        if (buildFile.isFile()) {
            // This is a gradle build directory
            // -> Analyze and send it to the callback, no recursion

            analyzer.analyzeProjectDir(baseDir).ifPresent(callback);
        } else {
            // This is not a gradle build directory
            // -> continue search recursively

            File[] subDirs = baseDir.listFiles(File::isDirectory);

            for (File subDir : subDirs) {
                scanChildren(subDir, callback);
            }
        }
    }
}
