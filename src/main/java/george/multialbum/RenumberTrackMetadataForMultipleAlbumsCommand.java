package george.multialbum;

import cli.pi.AppInfo;
import cli.pi.command.CliCommand;
import cli.pi.command.CommandContext;
import com.github.born2snipe.cli.CountUpToTotalPrinter;
import net.sourceforge.argparse4j.inf.Namespace;
import org.apache.commons.io.FileUtils;
import org.kohsuke.MetaInfServices;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

@MetaInfServices
public class RenumberTrackMetadataForMultipleAlbumsCommand extends CliCommand {
    public RenumberTrackMetadataForMultipleAlbumsCommand() {
        argsParser.addArgument("-i", "--input-dir")
                .required(true)
                .dest("input")
                .help("Path to directory to copy files from");

        argsParser.addArgument("-o", "--output-dir")
                .required(true)
                .dest("output")
                .help("Path to directory to copy files to");
    }

    @Override
    public String getName() {
        return "renumber-track-metadata-for-multi-albums";
    }

    @Override
    public String getDescription() {
        return AppInfo.get("multialbum.update.metadata.description");
    }

    @Override
    protected void executeParsedArgs(CommandContext commandContext) {
        Namespace namespace = commandContext.getNamespace();
        File inputDir = new File(namespace.getString("input"));
        File outputDir = new File(namespace.getString("output"));

        if (!inputDir.exists()) {
            throw new DirectoryDoesNotExistException(inputDir);
        }
        if (!outputDir.exists()) {
            throw new DirectoryDoesNotExistException(outputDir);
        }

        File[] filesToProcess = inputDir.listFiles();

        CountUpToTotalPrinter progressPrinter = new CountUpToTotalPrinter(filesToProcess.length);

        commandContext.getLog().warn("Copying {0} file(s)...", filesToProcess.length);

        Arrays.stream(filesToProcess)
                .filter(this::isNotDirectory)
                .map((inputFile) -> copyFileTo(inputFile, outputDir))
                .peek((outputFile) -> progressPrinter.step())
                .collect(Collectors.toList())
        ;
    }

    private boolean isNotDirectory(File file) {
        return !file.isDirectory();
    }

    private File copyFileTo(File inputFile, File outputDir) {
        File outputFile = new File(outputDir, inputFile.getName());
        try {
            FileUtils.copyFile(inputFile, outputFile);
            return outputFile;
        } catch (IOException e) {
            throw new RuntimeException("Failed to write file: " + outputFile.getAbsolutePath(), e);
        }
    }
}
