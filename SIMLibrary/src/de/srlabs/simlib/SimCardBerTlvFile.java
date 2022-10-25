package de.srlabs.simlib;

import javax.smartcardio.CardException;

public class SimCardBerTlvFile extends SimCardElementaryFile {

    public SimCardBerTlvFile(SelectResponse selectResponse) throws CardException {
        super(selectResponse);
        System.err.println(LoggingUtils.formatDebugMessage("not yet implemented, a blank class, FileID: " + selectResponse.getFileId()));
    }
}