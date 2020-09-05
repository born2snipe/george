package george.multialbum;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DiskTrackTest {
    @Test
    public void tracksAreSortable() {
        DiskTrack track1 = new DiskTrack(new File("1-1 blah.m4a"));
        DiskTrack track2 = new DiskTrack(new File("2-2 blah.m4a"));
        DiskTrack track3 = new DiskTrack(new File("3-1 blah.m4a"));
        DiskTrack track4 = new DiskTrack(new File("03-000010 blah.m4a"));

        ArrayList<DiskTrack> actualTracks = new ArrayList(Arrays.asList(track4, track2, track1, track3));
        Collections.sort(actualTracks);

        assertEquals(Arrays.asList(track1, track2, track3, track4), actualTracks);
    }

    @Test
    public void with_zero_padding() {
        DiskTrack track = new DiskTrack(new File("050-011 blah.m4a"));

        assertEquals(50, track.getDisk());
        assertEquals(11, track.getTrack());
    }

    @Test
    public void no_zero_padding() {
        DiskTrack track = new DiskTrack(new File("1-1 blah.m4a"));

        assertEquals(1, track.getDisk());
        assertEquals(1, track.getTrack());
    }
}