package george.multialbum;

import cli.pi.AppInfo;
import cli.pi.command.CliCommand;
import cli.pi.command.CommandContext;
import com.github.born2snipe.cli.CountUpToTotalPrinter;
import net.sourceforge.argparse4j.inf.Namespace;
import org.apache.commons.io.FileUtils;
import org.kohsuke.MetaInfServices;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static reactor.core.scheduler.Schedulers.parallel;

@MetaInfServices
public class RenumberTrackMetadataForMultipleAlbumsCommand extends CliCommand {
    public static final String AUDIO_FILE_EXTENSION = "m4a";

    private final Scheduler scheduler = parallel();

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
            outputDir.mkdirs();
        }

        List<File> audioFiles = Arrays.stream(inputDir.listFiles())
                .filter(this::isNotDirectory)
                .filter(this::isAudioFile)
                .collect(Collectors.toList());

        CountUpToTotalPrinter progressPrinter = new CountUpToTotalPrinter(audioFiles.size());
        commandContext.getLog().warn("Copying {0} file(s)...", audioFiles.size());

        Flux.fromIterable(audioFiles)
                .flatMap((inputFile) -> copyFileTo(inputFile, outputDir))
                .doOnNext((outputFile) -> progressPrinter.step())
                .subscribeOn(scheduler)
                .blockLast()
        ;
    }

    private boolean isAudioFile(File file) {
        return file.getName().endsWith("." + AUDIO_FILE_EXTENSION);
    }

    private boolean isNotDirectory(File file) {
        return !file.isDirectory();
    }

    private Mono<File> copyFileTo(File inputFile, File outputDir) {
        return Mono.fromCallable(() -> {
            File outputFile = new File(outputDir, inputFile.getName());

            try {
                FileUtils.copyFile(inputFile, outputFile);
                return outputFile;
            } catch (IOException e) {
                throw new RuntimeException("Failed to write file: " + outputFile.getAbsolutePath(), e);
            }
        }).subscribeOn(scheduler);

    }
}
