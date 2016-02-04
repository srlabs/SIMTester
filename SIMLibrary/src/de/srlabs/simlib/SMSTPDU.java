package de.srlabs.simlib;

public class SMSTPDU {

    private final static byte TAG_GSM = (byte) 0x0B;
    private final static byte TAG_3G = (byte) 0x8B;
    private byte _length; //FIXME: can this be on 2 bytes? 
    private byte[] _tpdu;

    public SMSTPDU() {
    }

    public SMSTPDU(byte[] tpdu) {
        setTPDU(tpdu);
    }

    public int getLength() {
        return _length;
    }

    public byte[] getTPDU() {
        return _tpdu;
    }

    public final void setTPDU(byte[] tpdu) {
        _tpdu = new byte[tpdu.length];
        System.arraycopy(tpdu, 0, _tpdu, 0, tpdu.length);
        _length = (byte) tpdu.length;
    }

    public byte[] getBytes() {
        byte[] output = new byte[2 + _tpdu.length];
        if (SIMLibrary.third_gen_apdu) {
            output[0] = TAG_3G;
        } else {
            output[0] = TAG_GSM;
        }
        output[1] = _length;
        System.arraycopy(_tpdu, 0, output, 2, _tpdu.length);

        return output;
    }
}
