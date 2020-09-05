package george.multialbum;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;

import java.io.File;

public class UpdateTrackNumberInMetaData {
    public void update(File audioFile, String trackNumber) {
        try {
            AudioFile f = AudioFileIO.read(audioFile);
            Tag tag = f.getTag();
            tag.setField(FieldKey.TRACK, trackNumber);
            f.commit();
        } catch (Exception e) {
            throw new IllegalStateException("Failed updating track number (" + trackNumber + ") for: " + audioFile.getName(), e);
        }
    }
}
