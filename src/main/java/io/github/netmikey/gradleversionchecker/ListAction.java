package io.github.netmikey.gradleversionchecker;

import static org.fusesource.jansi.Ansi.*;

import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Encapsulates the logic of the list action.
 */
@Component
public class ListAction implements Consumer<ProjectMetadata> {

    @Autowired
    private ConsoleWriter out;

    private int numFound;

    @Override
    public void accept(ProjectMetadata project) {
        numFound++;

        out.println(">> Project: " + project.getProjectDir().getPath());
        out.println(ansi().a("    Gradle: ").bold().format("%-10s ", project.getGradleVersion()).boldOff()
            .a(out.formatVersionUpToDate(project)));
        if (project.isUnderGitVersionControl()) {
            out.println(ansi().a("    Branch: ").format("%-10s ", project.getBranch()));
        }
        out.println("");
    }

    /**
     * Get the numFound.
     * 
     * @return Returns the numFound.
     */
    public int getNumFound() {
        return numFound;
    }

}
