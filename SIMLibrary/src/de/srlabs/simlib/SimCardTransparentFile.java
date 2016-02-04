package de.srlabs.simlib;

import javax.smartcardio.CardException;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

public class SimCardTransparentFile extends SimCardElementaryFile {

    private final static boolean LOCAL_DEBUG = false;
    private final static boolean DEBUG = Debug.DEBUG || LOCAL_DEBUG;

    public SimCardTransparentFile(SelectResponse selectResponse) {
        super(selectResponse);
    }

    public byte[] getContent() throws CardException {
        if (DEBUG) {
            System.out.println(LoggingUtils.formatDebugMessage("reading content (length=" + _fileSize + ")"));
        }
        return getContent(_fileSize);
    }

    public byte[] getContent(int length) throws CardException {
        return getContent(0, length);
    }

    public byte[] getContent(int offset, int length) throws CardException {
        /* FIXME: file reading by offset, not needed for now, we're reading the whole files
         * P1P2 (GSM):
         *
         * P1 and P2 define the part of the file data to be read:
         * OffsetH is the high (P1), or most significant byte (MSB) of the file
         * OffsetL is the low (P2), or least significant byte (LSB) of the file
         */
        CommandAPDU readBinary;
        if (SIMLibrary.third_gen_apdu) {
            readBinary = new CommandAPDU((byte) 0x00, (byte) 0xB0, (byte) 0x00, (byte) 0x00, (byte) length); // Read Binary APDU
        } else {
            readBinary = new CommandAPDU((byte) 0xA0, (byte) 0xB0, (byte) 0x00, (byte) 0x00, (byte) length); // Read Binary APDU
        }
        ResponseAPDU response = ChannelHandler.transmitOnDefaultChannel(readBinary);

        switch ((short) response.getSW()) {
            case (short) 0x9000:
                return response.getData();
            case (short) 0x9804: // 9804h - Security status not satisfied
            case (short) 0x6982: // 6982h - Security status not satisfied (3G)
                System.err.println(LoggingUtils.formatDebugMessage("security problem during reading content of a file " + _fileId + " perhaps you need to enter PIN or auth via ADM to read this file, check ARR"));
                return null;
            default:
                throw new CardException("an unexpected error has occured during reading content of a file " + _fileId + "; SW = " + String.format("%02X", response.getSW()));
        }

    }
}
