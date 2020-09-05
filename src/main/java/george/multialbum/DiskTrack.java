package george.multialbum;

import java.io.File;
import java.util.Objects;

public class DiskTrack implements Comparable<DiskTrack> {
    private final File file;
    private final Integer disk;
    private final Integer track;

    public DiskTrack(File file) {
        this.file = file;
        String fileName = file.getName();
        disk = Integer.valueOf(fileName.replaceAll("(^.+?)-.+", "$1"));
        track = Integer.valueOf(fileName.replaceAll("^.+?-(.+?) .+", "$1"));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DiskTrack that = (DiskTrack) o;
        return file.equals(that.file);
    }

    @Override
    public int hashCode() {
        return Objects.hash(file);
    }

    public int getDisk() {
        return disk;
    }

    public int getTrack() {
        return track;
    }

    public File getFile() {
        return file;
    }

    @Override
    public String toString() {
        return "DiskTrack{" +
                "file=" + file +
                ", disk=" + disk +
                ", track=" + track +
                '}';
    }

    @Override
    public int compareTo(DiskTrack o) {
        int result = disk.compareTo(o.disk);
        if (result == 0) {
            return track.compareTo(o.track);
        }
        return result;
    }
}
