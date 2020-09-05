package george;

import cli.pi.command.CliCommand;
import cli.pi.command.CommandContext;
import org.kohsuke.MetaInfServices;

@MetaInfServices
public class TestCommand extends CliCommand {
    @Override
    public String getName() {
        return "test";
    }

    @Override
    public String getDescription() {
        return "blah blah blah";
    }

    @Override
    protected void executeParsedArgs(CommandContext commandContext) {

    }
}
