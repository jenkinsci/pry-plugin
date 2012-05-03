package org.jenkinsci.plugins.pry;

import org.jruby.CompatVersion;
import org.jruby.Ruby;
import org.jruby.RubyIO;
import org.jruby.embed.LocalContextScope;
import org.jruby.embed.ScriptingContainer;

import java.io.File;

/**
 * Stand-alone local test.
 *
 * @author Kohsuke Kawaguchi
 */
public class Main {

    private ScriptingContainer ruby;

    public static void main(String[] args) throws Exception {
        new Main().run();
    }

    public void run() throws Exception {
        ruby = new ScriptingContainer(LocalContextScope.SINGLETHREAD);
        ruby.setCompatVersion(CompatVersion.RUBY1_9);

        File gemsHome = resolve(new File("./target/classes"), "pry-gems");
        addLoadPath(gemsHome.getAbsolutePath());
        addLoadPath("pry-gems");

        require("launcher");
        Ruby vm = ruby.getRuntime();
        callMethod(eval("Launcher"),"start",new RubyIO(vm, System.in), new RubyIO(vm, System.out));
    }

    private void addLoadPath(String path) {
   		callMethod(eval("$:"), "unshift", path);
   	}

    private Object eval(String script) {
   		return this.ruby.runScriptlet(script);
   	}

    public Object callMethod(Object object, String methodName, Object... args) {
   		return ruby.callMethod(object, methodName, args);
   	}

   	private void require(String path) {
   		eval("require '" + path + "'");
   	}

    private static File resolve(File base, String relative) {
        File rel = new File(relative);
        if(rel.isAbsolute())
            return rel;
        else
            return new File(base,relative);
    }

}
