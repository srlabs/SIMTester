package de.srlabs.simlib;

import javax.smartcardio.CardException;

public class SimCardNoInfoFile extends SimCardElementaryFile {

    public SimCardNoInfoFile(SelectResponse selectResponse) throws CardException {
        super(selectResponse);
        System.err.println(LoggingUtils.formatDebugMessage("not yet implemented, a blank class, FileID: " + selectResponse.getFileId()));
    }
}
