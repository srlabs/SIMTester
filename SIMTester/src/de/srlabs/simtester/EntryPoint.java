package de.srlabs.simtester;

import de.srlabs.simlib.CommandPacket;
import de.srlabs.simlib.ResponsePacket;
import java.util.Arrays;

public class EntryPoint {

    private byte[] _TAR;
    private int _keyset;
    private CommandPacket _cp;
    private ResponsePacket _rp;

    public EntryPoint(byte[] TAR, int keyset, CommandPacket cp, ResponsePacket rp) {
        if (TAR.length != 3) {
            throw new IllegalArgumentException();
        }
        if (keyset < 0 || keyset > 15) {
            throw new IllegalArgumentException();
        }
        _TAR = TAR;
        _keyset = keyset;
        _cp = cp;
        _rp = rp;
    }

    public byte[] getTAR() {
        return _TAR;
    }

    public int getKeyset() {
        return _keyset;
    }

    public CommandPacket getCommandPacket() {
        return _cp;
    }

    public ResponsePacket getResponsePacket() {
        return _rp;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(_TAR);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof EntryPoint)) {
            return false;
        }

        EntryPoint other = (EntryPoint) obj;
        return Arrays.equals(getTAR(), other.getTAR());
    }
}
