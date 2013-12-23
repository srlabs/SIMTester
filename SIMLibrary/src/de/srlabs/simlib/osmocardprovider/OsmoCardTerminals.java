package de.srlabs.simlib.osmocardprovider;

import de.srlabs.simlib.Debug;
import java.util.ArrayList;
import java.util.List;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CardTerminals;

public class OsmoCardTerminals extends CardTerminals {

    private final static boolean LOCAL_DEBUG = false;
    private final static boolean DEBUG = Debug.DEBUG || LOCAL_DEBUG;
    final static CardTerminals INSTANCE = new OsmoCardTerminals();

    public OsmoCardTerminals() {
    }

    @Override
    public List<CardTerminal> list(CardTerminals.State state) throws CardException {
        if (state != CardTerminals.State.ALL) {
            throw new CardException("OsmoCardTerminals doesn't support statuses!");
        }
        List<CardTerminal> list = new ArrayList<>(1);
        list.add(OsmoCardTerminal.getInstance());
        return list;
    }

    @Override
    public boolean waitForChange(long timeout) throws CardException {
        throw new CardException("waitForChange() not implemented in OsmoCardTerminals");
    }
}
