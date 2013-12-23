package de.srlabs.simtester;

import de.srlabs.simlib.HexToolkit;
import java.util.Comparator;

public class FuzzerResultComparator implements Comparator<FuzzerResult> {

    @Override
    public int compare(FuzzerResult o1, FuzzerResult o2) {
        int byTARs = HexToolkit.compareTARs(o1._commandPacket.getTAR(), o2._commandPacket.getTAR());
        if (byTARs == 0) {
            return o1._commandPacket.getKeyset() - o2._commandPacket.getKeyset();
        } else {
            return byTARs;
        }
    }
}
