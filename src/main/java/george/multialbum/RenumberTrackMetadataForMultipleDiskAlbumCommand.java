package george.multialbum;

import cli.pi.AppInfo;
import cli.pi.CliLog;
import cli.pi.command.CliCommand;
import cli.pi.command.CommandContext;
import com.github.born2snipe.cli.CountUpToTotalPrinter;
import net.sourceforge.argparse4j.inf.Namespace;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.MetaInfServices;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.stream.Collectors.toList;
import static reactor.core.scheduler.Schedulers.parallel;

@MetaInfServices
public class RenumberTrackMetadataForMultipleDiskAlbumCommand extends CliCommand {
    public static final String AUDIO_FILE_EXTENSION = "m4a";

    private final Scheduler scheduler = parallel();
    private UpdateTrackNumberInMetaData updateTrackNumberInMetaData = new UpdateTrackNumberInMetaData();

    public RenumberTrackMetadataForMultipleDiskAlbumCommand() {
        argsParser.addArgument("-i", "--input-dir")
                .metavar("PATH")
                .required(true)
                .dest("input")
                .help("Path to directory to copy files from");

        argsParser.addArgument("-o", "--output-dir")
                .metavar("PATH")
                .required(true)
                .dest("output")
                .help("Path to directory to copy files to");
    }

    @Override
    public String getName() {
        return "renumber-track-metadata";
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
            if (outputDir.mkdirs()) {
                commandContext.getLog().info("Created output directory...");
            } else {
                commandContext.getLog().warn("Failed making output directory!");
            }
        }

        List<DiskTrack> tracks = copyFilesToOutputDir(commandContext, inputDir, outputDir);
        updateMetadata(commandContext, tracks);
    }

    private void updateMetadata(CommandContext commandContext, List<DiskTrack> tracks) {
        Collections.sort(tracks);

        CountUpToTotalPrinter progressPrinter = new CountUpToTotalPrinter(tracks.size());
        progressPrinter.setMessageFormat("Metadata Updated: {count} of {total}");
        commandContext.getLog().warn("Updating track metadata...");

        for (int i = 0; i < tracks.size(); i++) {
            File file = tracks.get(i).getFile();
            String trackNumber = StringUtils.leftPad(String.valueOf(i + 1), 3, "0");
            updateTrackNumberInMetaData.update(file, trackNumber);
            progressPrinter.println("Track " + trackNumber + " for " + file.getName());
            progressPrinter.step();
        }
    }

    private List<DiskTrack> copyFilesToOutputDir(CommandContext commandContext, File inputDir, File outputDir) {
        File[] filesOnDisk = inputDir.listFiles();
        if (filesOnDisk == null) {
            throw new IllegalStateException("For some reason we are unable to get the list of files on disk.\n" +
                    inputDir.getAbsolutePath() +
                    "\n ¯\\_(ツ)_/¯");
        }

        List<File> audioFiles = Arrays.stream(filesOnDisk)
                .filter(this::isNotDirectory)
                .filter(this::isAudioFile)
                .filter(this::isAudioFileThatIsPartOfTheDiskSet)
                .collect(toList());

        if (audioFiles.isEmpty()) {
            commandContext.getLog().warn("No audio files found to copy.");
            return Collections.emptyList();
        }

        CountUpToTotalPrinter progressPrinter = new CountUpToTotalPrinter(audioFiles.size());
        progressPrinter.setMessageFormat("Copied: {count} of {total}");

        commandContext.getLog().warn("Copying {0} file(s)...", audioFiles.size());

        return Flux.fromIterable(audioFiles)
                .flatMap((inputFile) -> copyFileTo(inputFile, outputDir))
                .doOnNext((outputFile) -> {
                    progressPrinter.println("Copied: " + outputFile.getName());
                    progressPrinter.step();
                })
                .map(DiskTrack::new)
                .subscribeOn(scheduler)
                .collectList()
                .block();
    }

    private boolean isAudioFileThatIsPartOfTheDiskSet(File file) {
        boolean matchesPattern = file.getName().matches("[0-9]+?-[0-9]+? .+?\\." + AUDIO_FILE_EXTENSION);
        if (!matchesPattern) {
            new CliLog().warn("Ignoring file ({0}), since the filename does NOT match the correct pattern", file.getName());
        }
        return matchesPattern;
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
                Files.copy(inputFile.toPath(), outputFile.toPath(), REPLACE_EXISTING, COPY_ATTRIBUTES);
                return outputFile;
            } catch (IOException e) {
                throw new RuntimeException("Failed to write file: " + outputFile.getAbsolutePath(), e);
            }
        }).subscribeOn(scheduler);

    }
}

