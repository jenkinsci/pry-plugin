package org.jenkinsci.plugins.pry;

import java.util.List;

/**
 * @author Kohsuke Kawaguchi
 */
public interface ReadlineCompleter {
    List<String> complete(String s);
}
