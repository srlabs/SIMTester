package de.srlabs.simlib.osmocardprovider;

import javax.smartcardio.CardTerminals;
import javax.smartcardio.TerminalFactorySpi;

public class OsmoSpi extends TerminalFactorySpi {
    
    public OsmoSpi (Object params) {
    
    }
    
    @Override
    public CardTerminals engineTerminals() {
        return OsmoCardTerminals.INSTANCE;
    }

}
