package io.github.netmikey.gradleversionchecker;

import java.io.File;

/**
 * The application's main class.
 */
public class Main {

    /**
     * The java main method.
     * 
     * @param args
     *            Command-line arguments.
     */
    public static void main(String[] args) {
        ConsoleWriter out = ConsoleWriter.INSTANCE;
        ProjectAnalyzer analyzer = new ProjectAnalyzer(out);
        Scanner scanner = new Scanner(out, analyzer);
        scanner.scan(new File("."));
        out.println("\n" + analyzer.getNumFound() + " Gradle project" +
            (analyzer.getNumFound() > 1 ? "s" : "") + " found");
    }

}
