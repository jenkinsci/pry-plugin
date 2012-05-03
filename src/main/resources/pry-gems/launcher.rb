require 'bundler/setup'
require 'pry'

class Launcher
    # bootstraps Pry.
    def self.start(root,i,o,cmd)
        Pry.config.pager=false;
        o.sync = true;      # otherwise you don't see output until it spills from the buffer

        i.singleton_class.class_eval do
            attr_accessor :out, :cmd
            attr_accessor :completion_proc

            def readline(prompt)
                if cmd then
                    cmd.readline(prompt,completion_proc)
                else
                    out.write prompt
                    gets
                end
            end

#            def completion_proc= (p)
#                # see http://www.ruby-doc.org/stdlib-1.9.3/libdoc/readline/rdoc/Readline.html#method-c-completion_proc-3D
#                out.puts "proc=#{p}"
#            end
        end
        i.out = o
        i.cmd = cmd

        Pry.start(root, :input => i, :output => o)
    end
end