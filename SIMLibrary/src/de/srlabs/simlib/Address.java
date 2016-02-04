package de.srlabs.simlib;

public class Address {

    private byte _type = TYPE_GSM; // default is GSM
    private byte _length; // FIXME: could possibly be more than 1 byte but it's not going to be the case for current usage
    public final static byte TYPE_GSM = (byte) 0x06;
    public final static byte TYPE_3G = (byte) 0x86;
    private byte _ton_npi;
    private byte[] _dialing_string;

    // TON values (b7, b6, b5 bites)
    /*
     * 000: Unknown;
     * 001: International Number;
     * 010: National Number;
     * 011: Network Specific Number;
     */
    // NPI values (b4, b3, b2, b1 bites)
    /*
     * 0000: Unknown;
     * 0001: ISDN/telephony numbering plan (ITU-T Recommendations E.164 [22] and E.163 [41]);
     * 0011: Data numbering plan (ITU-T Recommendation X.121 [23]);
     * 0100: Telex numbering plan (ITU-T Recommendation F.69 [24]);
     * 1001: Private numbering plan;
     */
    public Address(String TON, String NPI, String dialingNumber) throws Exception {
        throw new Exception("not yet implemented");
    }

    public Address(byte type, String TON, String NPI, String dialingNumber) throws Exception {
        throw new Exception("not yet implemented");
    }

    public Address(byte[] rawData) {
        if (rawData.length > 4) {
            if (rawData[0] == TYPE_GSM || rawData[0] == TYPE_3G) {
                _type = rawData[0];
                if (rawData[1] == (rawData.length - 2)) {
                    _length = rawData[1];
                    _ton_npi = rawData[2];
                    _dialing_string = new byte[rawData.length - 3]; // FIXME: should be MAX 20 nibbles!!
                    System.arraycopy(rawData, 3, _dialing_string, 0, rawData.length - 3);
                } else {
                    throw new IllegalArgumentException("rawData don't correspond with a length entered as 2nd byte!");
                }
            } else {
                throw new IllegalArgumentException("rawData don't start with a correct tag!");
            }
        } else {
            throw new IllegalArgumentException("rawData are shorter than TAG+LENGTH+TON_NPI+DIALINGSTRING");
        }
    }

    public void getTON() throws Exception {
        throw new Exception("not yet implemented");
    }

    public void getNPI() throws Exception {
        throw new Exception("not yet implemented");
    }

    public byte[] getDialingString() {
        return _dialing_string;
    }

    public byte[] getBytes() {
        byte[] address = new byte[1 + 1 + 1 + _dialing_string.length]; // tag + length + ton_npi + dialing_string
        address[0] = _type;
        address[1] = _length;
        address[2] = _ton_npi;
        System.arraycopy(_dialing_string, 0, address, 3, _dialing_string.length);
        return address;
    }

    public int getLength() {
        return _length;
    }
}
