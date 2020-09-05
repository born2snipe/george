package george.multialbum;

import cli.pi.command.ArgsParsingException;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RenumberTrackMetadataForMultipleAlbumsCommandTest {
    public static final String AUDIO_FILE_EXTENSION = "m4a";

    @TempDir
    Path temp;
    private RenumberTrackMetadataForMultipleAlbumsCommand cmd;
    private File rootDir;
    private File outputDir;
    private File inputDir;

    @BeforeEach
    void setUp() {
        cmd = new RenumberTrackMetadataForMultipleAlbumsCommand();

        rootDir = temp.toFile();
        inputDir = new File(rootDir, "input");
        outputDir = new File(rootDir, "output");

        inputDir.mkdirs();
        outputDir.mkdirs();
    }

    @Test
    public void ignore_audio_files_that_do_not_have_the_correct_filename_prefix() {
        File randomTrack = new File(inputDir, "should-not-be-copied." + AUDIO_FILE_EXTENSION);
        writeRandomValueToFile(randomTrack);

        cmd.execute("-i", inputDir.getAbsolutePath(), "-o", outputDir.getAbsolutePath());

        assertEquals(0, outputDir.listFiles().length);
    }

    @Test
    public void ignore_not_audio_files() {
        File nestedDir = new File(inputDir, "nested");
        nestedDir.mkdirs();

        List<File> inputFiles = generateRandomFilesIn(inputDir, 1, 1, "txt");

        cmd.execute("-i", inputDir.getAbsolutePath(), "-o", outputDir.getAbsolutePath());

        assertEquals(0, outputDir.listFiles().length);
    }

    @Test
    public void ignore_directories_in_the_input_directory() {
        File nestedDir = new File(inputDir, "nested");
        nestedDir.mkdirs();

        List<File> inputFiles = generateRandomFilesIn(inputDir, 1, 1, AUDIO_FILE_EXTENSION);

        cmd.execute("-i", inputDir.getAbsolutePath(), "-o", outputDir.getAbsolutePath());

        assertFilesCopied(inputFiles, outputDir);
    }

    @Test
    public void copy_files_to_output_dir() {
        List<File> inputFiles = generateRandomFilesIn(inputDir, 1, 1, AUDIO_FILE_EXTENSION);

        cmd.execute("-i", inputDir.getAbsolutePath(), "-o", outputDir.getAbsolutePath());

        assertFilesCopied(inputFiles, outputDir);
    }

    @Test
    public void no_args() {
        assertThrows(RuntimeException.class, () -> cmd.execute());
    }

    @Test
    public void input_dir_does_not_exist() {
        assertThrows(DirectoryDoesNotExistException.class, () -> cmd.execute(
                "-o", outputDir.getAbsolutePath(),
                "-i", new File(rootDir, "does-not-exist").getAbsolutePath()
        ));
    }

    @Test
    public void output_dir_does_not_exist() {
        File outputDir = new File(rootDir, "does-not-exist");
        cmd.execute("-i", inputDir.getAbsolutePath(), "-o", outputDir.getAbsolutePath());
        assertTrue(outputDir.exists());
    }

    @Test
    public void no_output_dir_provided() {
        assertThrows(ArgsParsingException.class, () -> cmd.execute("-i", inputDir.getAbsolutePath()));
    }

    @Test
    public void no_input_dir_provided() {
        assertThrows(ArgsParsingException.class, () -> cmd.execute("-o", outputDir.getAbsolutePath()));
    }

    private String checksum(File file) {
        try (InputStream input = new BufferedInputStream(new FileInputStream(file))) {
            return DigestUtils.sha256Hex(input);
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate checksum", e);
        }
    }

    private void writeRandomValueToFile(File file) {
        try {
            FileUtils.write(file, UUID.randomUUID().toString(), "UTF-8");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void assertFilesCopied(List<File> inputFiles, File outputDir) {
        inputFiles.forEach((expectedFile) -> {
            File actualFile = new File(outputDir, expectedFile.getName());
            assertTrue(actualFile.exists());
            assertEquals(checksum(expectedFile), checksum(actualFile));
        });
    }

    private List<File> generateRandomFilesIn(File inputDir, int discCount, int trackCount, String fileExtension) {
        ArrayList<File> trackFiles = new ArrayList<>();

        for (int i = 1; i <= discCount; i++) {
            for (int j = 0; j < trackCount; j++) {
                File file = new File(inputDir, i + "-" + StringUtils.leftPad(String.valueOf(j), 3, "0") + " " + UUID.randomUUID().toString() + "." + fileExtension);
                trackFiles.add(file);
                writeRandomValueToFile(file);
            }
        }

        return trackFiles;
    }
}