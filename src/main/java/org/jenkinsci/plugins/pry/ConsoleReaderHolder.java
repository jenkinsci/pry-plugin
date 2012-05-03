package org.jenkinsci.plugins.pry;

import jline.ConsoleReader;

import java.io.IOException;

/**
 * @author Kohsuke Kawaguchi
 */
public class ConsoleReaderHolder {
    public static final ConsoleReader INSTANCE = init();

    private static ConsoleReader init() {
        try {
            return new ConsoleReader();
        } catch (IOException e) {
            throw new Error(e);
        }
    }
}
