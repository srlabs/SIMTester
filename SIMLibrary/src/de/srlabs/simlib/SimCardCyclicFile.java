package de.srlabs.simlib;

import javax.smartcardio.CardException;

public class SimCardCyclicFile extends SimCardElementaryFile {
    
    public SimCardCyclicFile(SelectResponse selectResponse) throws CardException {
        super(selectResponse);
        System.err.println(LoggingUtils.formatDebugMessage("not yet implemented, a blank class, FileID: " + selectResponse.getFileId()));
    }
    
}
