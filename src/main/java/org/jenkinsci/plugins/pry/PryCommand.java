package org.jenkinsci.plugins.pry;

import hudson.Extension;
import hudson.Proc;
import hudson.cli.CLICommand;
import hudson.remoting.Callable;
import jenkins.model.Jenkins;
import jline.Completor;
import jline.ConsoleReader;
import jline.Terminal;
import jline.UnsupportedTerminal;
import org.jruby.Ruby;
import org.jruby.RubyHash;
import org.jruby.RubyHash.RubyHashEntry;
import org.jruby.RubyIO;
import org.jruby.RubyNil;
import org.jruby.RubyProc;
import org.jruby.embed.ScriptingContainer;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import ruby.ScriptingContainerHolder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * @author Kohsuke Kawaguchi
 */
@Extension
public class PryCommand extends CLICommand {
    private transient ScriptingContainer ruby;

    @Override
    public String getShortDescription() {
        return "Runs an interactive pry shell";
    }

    @Override
    public int main(List<String> args, Locale locale, InputStream stdin, PrintStream stdout, PrintStream stderr) {
        // this allows the caller to manipulate the JVM state, so require the admin privilege.
        Jenkins.getInstance().checkPermission(Jenkins.ADMINISTER);
        // TODO: ^as this class overrides main() (which has authentication stuff),
        // how to get ADMIN permission for this command?

        // this being remote means no jline capability is available
        System.setProperty("jline.terminal", UnsupportedTerminal.class.getName());
        Terminal.resetTerminal();

        ruby = new ScriptingContainerHolder().ruby;

        File gemsHome = resolve(getScriptDir(), "pry-gems");
        addLoadPath(gemsHome.getAbsolutePath());
        addLoadPath("pry-gems");

        require("launcher");
        Ruby vm = ruby.getRuntime();
        callMethod(eval("Launcher"), "start", new RubyIO(vm, stdin), new RubyIO(vm, stdout), this);
        return 0;
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

    /**
     * Returns a directory that stores all the Ruby scripts.
     */
    public File getScriptDir() {
        URL url = Jenkins.getInstance().getPluginManager().getPlugin("pry").baseResourceURL;
        // we assume url to be file:// path because we later need to be able to enumerate them
        // to lift this limitation, we need build-time processing to enumerate all the rb files.
        if (!url.getProtocol().equals("file"))
            throw new IllegalStateException("Unexpected base resource URL: "+url);

        return new File(new File(url.getPath()),"WEB-INF/classes");
    }

    protected int run() {
        throw new UnsupportedOperationException();
    }

    public String readline(final String prompt, final RubyProc completer) throws IOException, InterruptedException {
        try {
            ReadlineCompleter rc = new ReadlineCompleter() {
                public List<String> complete(String s) {
                    Ruby vm = completer.getRuntime();
                    IRubyObject out = completer.call(vm.getCurrentContext(), new IRubyObject[]{vm.newString(s)});
                    System.out.println(out);
                    List<String>  r = new ArrayList<String>();
                    for (Object o : (List)out.toJava(List.class)) {
                        r.add(o.toString());
                    }
                    return r;
                }
            };

            ReadlineCompleter proxy = channel.export(ReadlineCompleter.class,rc);
            return channel.call(new ReadlineTask(proxy, prompt));
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw e;
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw e;
        }
    }

    private static class ReadlineTask implements Callable<String, IOException> {
        private final ReadlineCompleter proxy;
        private final String prompt;

        public ReadlineTask(ReadlineCompleter proxy, String prompt) {
            this.proxy = proxy;
            this.prompt = prompt;
        }

        public String call() throws IOException {
            ConsoleReader cr = new ConsoleReader();
            cr.addCompletor(new Completor() {
                public int complete(String buffer, int cursor, List candidates) {
                    candidates.addAll(proxy.complete(buffer));
                    return 0;
                }
            });
            return cr.readLine(prompt);
        }

        private static final long serialVersionUID = 1L;
    }
}
