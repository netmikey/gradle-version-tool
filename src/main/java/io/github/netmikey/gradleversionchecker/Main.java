package io.github.netmikey.gradleversionchecker;

import java.io.File;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The application's main class.
 */
@SpringBootApplication
public class Main implements CommandLineRunner {

    @Autowired
    private Scanner scanner;

    @Autowired
    private ProjectAnalyzer analyzer;

    @Autowired
    private ConsoleWriter out;

    /**
     * The java main method.
     * 
     * @param args
     *            Command-line arguments.
     */
    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        scanner.scan(new File("."));
        out.println("\n" + analyzer.getNumFound() + " Gradle project" +
            (analyzer.getNumFound() > 1 ? "s" : "") + " found");
    }

}
