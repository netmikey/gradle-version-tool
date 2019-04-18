package io.github.netmikey.gradleversionchecker;

import org.junit.jupiter.api.Test;

/**
 * Run the version tool on this very project.
 */
public class SelfTest {

    /**
     * Run the version tool on this very project in list mode.
     */
    @Test
    public void testListActionOnMyself() {
        Main.main(new String[0]);
    }
}
