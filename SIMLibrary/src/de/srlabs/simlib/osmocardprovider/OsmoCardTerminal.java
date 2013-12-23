package de.srlabs.simlib.osmocardprovider;

import de.srlabs.simlib.Debug;
import javax.smartcardio.Card;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;

public class OsmoCardTerminal extends CardTerminal {

    static {
        OsmoJNI.loadLib();
    }
    private final static boolean LOCAL_DEBUG = false;
    private final static boolean DEBUG = Debug.DEBUG || LOCAL_DEBUG;
    public static OsmoJNI _osmojni = new OsmoJNI();
    private static OsmoCardTerminal _instance = null;
    public static boolean _initialized = false;

    public static synchronized OsmoCardTerminal getInstance() throws CardException {
        if (null == _instance) {
            _instance = new OsmoCardTerminal();
            return _instance;
        } else {
            return _instance;
        }
    }

    private OsmoCardTerminal() {
    }

    @Override
    public String getName() {
        return "OsmoCardTerminal";
    }

    @Override
    public Card connect(String protocol) throws CardException {
        if (!_initialized) {
            if (_osmojni.init()) {
                _initialized = true;
            } else {
                throw new CardException("Unable to initialize osmosim, is Osmocom BB initialized with layer2 socket open?");
            }
            if (DEBUG) {
                _osmojni.loglevel(1); // DEBUG logging
            } 
        }

        return OsmoCard.getInstance();
    }

    @Override
    public boolean isCardPresent() throws CardException {
        throw new CardException("isCardPresent() not implemented in OsmoCardTerminal");
    }

    @Override
    public boolean waitForCardPresent(long l) throws CardException {
        throw new CardException("waitForCardPresent() not implemented in OsmoCardTerminal");
    }

    @Override
    public boolean waitForCardAbsent(long l) throws CardException {
        throw new CardException("waitForCardAbsent() not implemented in OsmoCardTerminal");
    }
}
