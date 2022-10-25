package de.srlabs.simlib;

import javax.smartcardio.CardException;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

public class SimCardLinearFixedFile extends SimCardElementaryFile {

    private final static boolean LOCAL_DEBUG = false;
    private final static boolean DEBUG = Debug.DEBUG || LOCAL_DEBUG;
    private int _recordLength;
    private int _numberOfRecords;

    public SimCardLinearFixedFile(SelectResponse selectResponse) {
        super(selectResponse);
        if (getFileStructure() == EF_LINEAR_FIXED) {
            if (SIMLibrary.third_gen_apdu) {
                byte[] file_desc = TLVToolkit.getTLV(selectResponse.getResponseData(), (byte) 0x82);
                if (DEBUG) {
                    System.out.println(LoggingUtils.formatDebugMessage("File Descriptor data (0x82): " + HexToolkit.toString(file_desc)));
                }
                if (file_desc[1] != (byte) 0x05) {
                    throw new IllegalStateException("FCM template does not contain record information, wth? FCM: " + HexToolkit.toString(file_desc));
                }
                _recordLength = ((byte) ((byte) file_desc[4] << 8) | (byte) file_desc[5]);
                _numberOfRecords = (byte) file_desc[6];
            } else {
                _recordLength = selectResponse.getResponseData()[14];
                _numberOfRecords = (_fileSize / _recordLength);
            }
        }

    }

    public int getRecordLength() throws UnsupportedOperationException {
        if (getFileType() == SimCardFile.EF && getFileStructure() == SimCardElementaryFile.EF_LINEAR_FIXED) {
            return _recordLength;
        } else {
            throw new UnsupportedOperationException("Selected file is not an EF or an EF is not a Linear-Fixed structed");
        }
    }

    public byte[] getFirstRecord() throws CardException {
        return getRecord(1);
    }

    public byte[] getRecord(int recordNr) throws CardException {

        CommandAPDU readRecord;
        if (SIMLibrary.third_gen_apdu) {
            readRecord = new CommandAPDU((byte) 0x00, (byte) 0xB2, (byte) recordNr, (byte) 0x04, (byte) _recordLength); // Read Record APDU
        } else {
            readRecord = new CommandAPDU((byte) 0xA0, (byte) 0xB2, (byte) recordNr, (byte) 0x04, (byte) _recordLength); // Read Record APDU
        }
        ResponseAPDU response = ChannelHandler.transmitOnDefaultChannel(readRecord);

        switch ((short) response.getSW()) {
            case (short) 0x9000:
                return response.getData();
            case (short) 0x9402: // 9402h - Out of file, record not found (invalid address)
            case (short) 0x6A83: // 6A83h - Out of file, record not found (invalid address) (3G)
                System.err.println(LoggingUtils.formatDebugMessage("problem during reading content of a file " + _fileId + ", recordNr = " + recordNr + ", recordLength = " + _recordLength + "; outOfFile - Record NOT found!"));
                return null;
            case (short) 0x9804: // 9804h - Security status not satisfied
            case (short) 0x6982: // 6982h - Security status not satisfied (3G)
                System.err.println(LoggingUtils.formatDebugMessage("security problem during reading content of a file " + _fileId + " perhaps you need to enter PIN or auth via ADM to read this file, check ARR"));
                return null;
            default:
                throw new CardException("an unexpected error has occured during reading content of a file " + _fileId + "; SW = " + Integer.toHexString(response.getSW()));
        }
    }

    public int getNumberOfRecords() {
        return _numberOfRecords;
    }
}
