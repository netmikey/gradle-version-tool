package io.github.netmikey.gradleversionchecker;

import static org.fusesource.jansi.Ansi.*;

import org.fusesource.jansi.Ansi.Erase;
import org.fusesource.jansi.AnsiConsole;
import org.springframework.stereotype.Component;

/**
 * Small wrapper around the Jansi library to handle console output.
 */
@Component
public class ConsoleWriter {

    /**
     * The singleton instance.
     */
    public static final ConsoleWriter INSTANCE = new ConsoleWriter();

    private boolean volatileState = false;

    private String lastVolatile = "";

    /**
     * Default constructor.
     */
    private ConsoleWriter() {
        AnsiConsole.systemInstall();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (volatileState) {
                System.out.print(ansi().eraseLine(Erase.ALL).cursorToColumn(0));
            }
        }));
    }

    /**
     * Print a volatile line.
     * 
     * @param message
     *            The message to be printed.
     */
    public void printVolatile(String message) {
        if (message.length() > 80) {
            message = message.substring(0, 77) + "...";
        }
        if (!lastVolatile.equals(message)) {
            System.out.print(ansi().eraseLine(Erase.ALL).cursorToColumn(0).a(message).cursorToColumn(0));
        }
        volatileState = true;
        lastVolatile = message;
    }

    /**
     * Print a regular, non-volatile line.
     * 
     * @param message
     *            The message to be printed.
     */
    public void println(Object message) {
        if (volatileState) {
            System.out.print(ansi().eraseLine(Erase.ALL).cursorToColumn(0));
        }
        System.out.println(message);
        volatileState = false;
        lastVolatile = "";
    }

}
