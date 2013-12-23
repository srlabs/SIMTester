package de.srlabs.simlib;

public class InnerTLV {

    /* 
     * First main purpose for this wrapper is special TLV length encoding as specified in TS 101 220, Section 7.1.2
     * 
     * This class should only be used for formatting the following TLV types:
     * 
     * Name of TLV          Encoding of tag field   Encoding of length field    Encoding of value field
     * BER-TLV              See ISO/IEC 8825-1 [15] See clause 7.1.2            See ISO/IEC 8825-1 [15]
     * COMPACT-TLV          See ISO/IEC 7816-4 [3]  See ISO/IEC 7816-4 [3]      See ISO/IEC 7816-4 [3]
     * COMPREHENSION-TLV    See clause 7.1.1        See clause 7.1.2            See ISO/IEC 7816-4 [3]
     * 
     * 7.1.2 Length encoding
     * The length is coded onto 1, 2, 3 or 4 bytes according to the following table:
     * Length                       Byte 1                  Byte 2                  Byte 3          Byte 4
     * 0 to 127                     Length ('00' to '7F')   Not present             Not present     Not present
     * 128 to 255                   '81'                    Length ('80' to 'FF')   Not present     Not present
     * 256 to 65 535                '82'                    Length ('01 00' to 'FF FF')             Not present
     * 65 536 to 16 777 215         '83'                    Length ('01 00 00' to 'FF FF FF')
     */
    public static byte[] getInnerTLV(int tag, byte[] data) throws IllegalArgumentException {
        byte[] apdu;

        if (data.length >= 0 && data.length <= 127) {
            apdu = new byte[1 + 1 + data.length];
            apdu[0] = (byte) tag;
            apdu[1] = (byte) data.length;
            System.arraycopy(data, 0, apdu, 2, data.length);
        } else if (data.length >= 128 && data.length <= 255) {
            apdu = new byte[1 + 2 + data.length];
            apdu[0] = (byte) tag;
            apdu[1] = (byte) 0x81;
            apdu[2] = (byte) data.length;
            System.arraycopy(data, 0, apdu, 3, data.length);
        } else if (data.length >= 256 && data.length <= 65535) {
            apdu = new byte[1 + 3 + data.length];
            apdu[0] = (byte) tag;
            apdu[1] = (byte) 0x82;
            apdu[2] = (byte) (data.length >> 8);
            apdu[3] = (byte) data.length;
            System.arraycopy(data, 0, apdu, 4, data.length);
        } else if (data.length >= 65536 && data.length <= 16777215) {
            apdu = new byte[1 + 4 + data.length];
            apdu[0] = (byte) tag;
            apdu[1] = (byte) 0x83;
            apdu[2] = (byte) (data.length >> 16);
            apdu[3] = (byte) (data.length >> 8);;
            apdu[4] = (byte) data.length;
            System.arraycopy(data, 0, apdu, 5, data.length);
        } else {
            throw new IllegalArgumentException("length of data entered is not supported!");
        }

        return apdu;
    }
}
