package io.github.netmikey.gradleversionchecker;

import java.io.File;

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
     */
    public void scan(File baseDir) {
        out.println("Scanning for Gradle projects in " + baseDir.getAbsolutePath() + " ...\n");
        scanChildren(baseDir);
    }

    private void scanChildren(File baseDir) {
        out.printVolatile(">> Scanning: " + baseDir.getAbsolutePath());

        if (!baseDir.isDirectory()) {
            return;
        }

        File buildFile = new File(baseDir, "build.gradle");

        if (buildFile.isFile()) {
            // This is a gradle build directory
            // -> Analyze and log it, no recursion

            analyzer.analyzeProjectDir(baseDir);
        } else {
            // This is not a gradle build directory
            // -> continue search recursively

            File[] subDirs = baseDir.listFiles(File::isDirectory);

            for (File subDir : subDirs) {
                scanChildren(subDir);
            }
        }
    }
}
