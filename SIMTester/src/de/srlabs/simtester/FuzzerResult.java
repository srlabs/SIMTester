package de.srlabs.simtester;

import de.srlabs.simlib.CommandPacket;
import de.srlabs.simlib.ResponsePacket;
import java.util.Arrays;

public class FuzzerResult {

    public CommandPacket _commandPacket;
    public FuzzerData _fuzzer;
    public ResponsePacket _responsePacket;

    public FuzzerResult(CommandPacket commandPacket, FuzzerData fuzzer, ResponsePacket responsePacket) {
        _commandPacket = commandPacket;
        _fuzzer = fuzzer;
        _responsePacket = responsePacket;
    }

    @Override
    public int hashCode() {
        int hash = 1;
        hash = hash * 17 + Arrays.hashCode(_commandPacket.getTAR());
        hash = hash * 31 + _commandPacket.getKeyset();
        hash = hash * 13 + Arrays.hashCode(_responsePacket.getBytes());
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof FuzzerResult)) {
            return false;
        }

        FuzzerResult other = (FuzzerResult) obj;
        return (Arrays.equals(_commandPacket.getTAR(), other._commandPacket.getTAR())
                && (_commandPacket.getKeyset() == other._commandPacket.getKeyset())
                && Arrays.equals(_responsePacket.getBytes(), other._responsePacket.getBytes()));
    }
}