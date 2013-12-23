package de.srlabs.simlib.osmocardprovider;

import de.srlabs.simlib.Debug;
import de.srlabs.simlib.LoggingUtils;
import javax.smartcardio.ATR;
import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;

public class OsmoCard extends Card {

    private final static boolean LOCAL_DEBUG = false;
    private final static boolean DEBUG = Debug.DEBUG || LOCAL_DEBUG;
    private ATR _atr = null;
    private static OsmoCard _instance = null;
    public static boolean _channelOpened = false;

    public static synchronized OsmoCard getInstance() {
        if (null == _instance) {
            _instance = new OsmoCard();
            return _instance;
        } else {
            return _instance;
        }
    }

    private OsmoCard() {
        getBasicChannel(); // open a channel immediately
    }

    @Override
    public ATR getATR() {
        if (null == _atr) throw new IllegalStateException("To get ATR you must open a channel first");
        return _atr;
    }

    @Override
    public String getProtocol() {
        return "there is no protocol";
    }

    @Override
    public final CardChannel getBasicChannel() {
        if (!_channelOpened) {
            byte[] byteATR = OsmoCardTerminal._osmojni.simPowerup();
            if (byteATR.length < 1) {
                System.err.println(LoggingUtils.formatDebugMessage("No card present as no ATR returned, exiting.."));
                System.exit(1);
            }
            _atr = new ATR(byteATR);
            _channelOpened = true;
        }
        return OsmoCardChannel.getInstance();
    }

    @Override
    public CardChannel openLogicalChannel() throws CardException {
        throw new CardException("openLogicalChannel() not implemented in OsmoCard");
    }

    @Override
    public void beginExclusive() throws CardException {
        throw new CardException("beginExclusive() not implemented in OsmoCard");
    }

    @Override
    public void endExclusive() throws CardException {
        throw new CardException("endExclusive() not implemented in OsmoCard");
    }

    @Override
    public byte[] transmitControlCommand(int controlCode, byte[] command) throws CardException {
        throw new CardException("transmitControlCommand() not implemented in OsmoCard");
    }

    @Override
    public void disconnect(boolean not_really_reset) throws CardException {
        // documentation says this should be TRUE to reset the card, but there is a bug in Java (reverse logic), nobody knows when and if it will ever get fixed (https://bugs.openjdk.java.net/show_bug.cgi?id=100151), 2012-04-01, the bug is still present in 1.6.0_31
        if (not_really_reset) {
            if (_channelOpened) {
                OsmoCardTerminal._osmojni.simPowerdown();
            }
            OsmoCardTerminal._osmojni.exit();
            OsmoCardTerminal._initialized = false;
        } else {
            boolean cardPresent = OsmoCardTerminal._osmojni.simReset();
            if (!cardPresent) {
                throw new CardException("Card is not present in the phone!");
            }
        }
    }
}
