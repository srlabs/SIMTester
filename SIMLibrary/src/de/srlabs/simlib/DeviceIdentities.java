package de.srlabs.simlib;

public class DeviceIdentities {

    private byte _type = TYPE_GSM; // default is GSM
    private final static byte _length = (byte) 0x02;
    private byte _source;
    private byte _destination;
    public final static byte TYPE_GSM = (byte) 0x02;
    public final static byte TYPE_3G = (byte) 0x82;
    public final static byte DI_UICC = (byte) 0x81; // '81' = UICC;
    public final static byte DI_TERMINAL = (byte) 0x82; // '82' = terminal;
    public final static byte DI_NETWORK = (byte) 0x83; // '83' = network;

    public DeviceIdentities(byte source, byte destination) {
        _source = source;
        _destination = destination;
    }

    public DeviceIdentities(byte type, byte source, byte destination) {
        this(source, destination);
        _type = type;
    }

    public byte[] getBytes() {
        byte[] device_identities = new byte[4];
        device_identities[0] = _type;
        device_identities[1] = _length;
        device_identities[2] = _source;
        device_identities[3] = _destination;
        return device_identities;
    }

    public byte getSource() {
        return _source;
    }

    public byte getDestination() {
        return _destination;
    }

    public int getLength() {
        return _length;
    }
}
