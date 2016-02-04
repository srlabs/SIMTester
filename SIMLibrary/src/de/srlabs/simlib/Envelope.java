package de.srlabs.simlib;

import javax.smartcardio.CommandAPDU;

public class Envelope {

    protected byte[] _data = null;

    public Envelope() {
    }

    public Envelope(byte[] data) {
        _data = data;
    }

    public CommandAPDU getAPDU() throws IllegalStateException {
        if (null == _data) {
            throw new IllegalStateException("Envelope: data variable has not been set!");
        }

        CommandAPDU _envelope_apdu;
        if (SIMLibrary.third_gen_apdu) {
            _envelope_apdu = new CommandAPDU((byte) 0x80, (byte) 0xC2, (byte) 0x00, (byte) 0x00, _data);
        } else {
            _envelope_apdu = new CommandAPDU((byte) 0xA0, (byte) 0xC2, (byte) 0x00, (byte) 0x00, _data);
        }

        return _envelope_apdu;
    }
}
