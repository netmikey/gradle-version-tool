package io.github.netmikey.gradleversionchecker;

import static org.fusesource.jansi.Ansi.*;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import javax.xml.ws.Holder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

/**
 * The application's main class.
 */
@SpringBootApplication
public class Main implements CommandLineRunner {

    @Autowired
    private Scanner scanner;

    @Autowired
    private ConsoleWriter out;

    @Autowired
    private UpgradeAction upgrade;

    @Value("${dir:.}")
    private String dir;

    @Value("${action:LIST}")
    private Action action;

    /**
     * The java main method.
     * 
     * @param args
     *            Command-line arguments.
     */
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(Main.class);
        applyDefaultProperties(application);
        application.run(args);
    }

    private static void applyDefaultProperties(SpringApplication application) {
        try {
            Properties props = PropertiesLoaderUtils.loadProperties(
                new ClassPathResource("/io/github/netmikey/gradleversionchecker/default-application.properties"));
            application.setDefaultProperties(props);
        } catch (IOException e) {
            throw new IllegalStateException("Error loading default-application.properties: " + e, e);
        }
    }

    @Override
    public void run(String... args) throws Exception {
        switch (action) {
            case LIST:
                list();
                break;
            case UPGRADE:
                scanner.scan(new File(dir), upgrade);
                break;
        }
    }

    private void list() {
        Holder<Integer> numFound = new Holder<>(0);
        scanner.scan(new File(dir), project -> {
            numFound.value++;

            out.println(">> Project: " + project.getProjectDir().getPath());
            out.println(ansi().a("    Gradle: ").bold().format("%-10s ", project.getGradleVersion()).boldOff()
                .a(out.formatVersionUpToDate(project)));
            if (project.isUnderGitVersionControl()) {
                out.println(ansi().a("    Branch: ").format("%-10s ", project.getBranch()));
            }
            out.println("");
        });
        out.println(numFound.value + " Gradle project" + (numFound.value > 1 ? "s" : "") + " found");
    }

    /**
     * The possible actions that the tool can execute.
     */
    public static enum Action {
        /**
         * List projects with Gradle versions.
         */
        LIST,

        /**
         * Upgrade Gradle versions.
         */
        UPGRADE;
    }
}
