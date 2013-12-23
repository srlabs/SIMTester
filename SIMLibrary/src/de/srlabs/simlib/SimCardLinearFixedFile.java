package de.srlabs.simlib;

import javax.smartcardio.CardException;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

public class SimCardLinearFixedFile extends SimCardElementaryFile {

    private int _recordLength;

    public SimCardLinearFixedFile(SelectResponse selectResponse) {
        super(selectResponse);
        if (getFileStructure() == EF_LINEAR_FIXED) {
            _recordLength = selectResponse.getResponseData()[14];
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
        CommandAPDU readRecord = new CommandAPDU((byte) 0xA0, (byte) 0xB2, (byte) recordNr, (byte) 0x04, (byte) _recordLength); // Read Record APDU
        ResponseAPDU response = ChannelHandler.getDefaultChannel().transmit(readRecord);

        switch ((short) response.getSW()) {
            case (short) 0x9000:
                return response.getData();
            case (short) 0x9402: // 9402h - Out of file, record not found (invalid address)
                System.err.println(LoggingUtils.formatDebugMessage("problem during reading content of a file " + _fileId + ", recordNr = " + recordNr + ", recordLength = " + _recordLength + "; outOfFile - Record NOT found!"));
                return null;
            case (short) 0x9804: // 9804h - Security status not satisfied
                System.err.println(LoggingUtils.formatDebugMessage("security problem during reading content of a file " + _fileId + " perhaps you need to enter PIN or auth via ADM to read this file, check ARR"));
                return null;
            default:
                throw new CardException("an unexpected error has occured during reading content of a file " + _fileId + "; SW = " + Integer.toHexString(response.getSW()));
        }
    }

    public int getNumberOfRecords() {
        return (_fileSize / _recordLength); // FIXME: check if this is always going to be precise
    }
}
