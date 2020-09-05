package george.multialbum;

import java.io.File;

public class DirectoryDoesNotExistException extends RuntimeException {
    public DirectoryDoesNotExistException(File dir) {
        super("Directory does not exist: " + dir.getAbsolutePath());
    }
}
