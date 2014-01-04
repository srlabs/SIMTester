package de.srlabs.simlib;

import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.List;
import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import javax.smartcardio.TerminalFactory;

public class ChannelHandler {

    private static ChannelHandler _instance = null;
    private static int _readerIndex = 0;
    private static String _terminalFactoryName;
    private CardChannel _cardChannel;
    private CardTerminal _cardTerminal;
    private static Card _card;
    private static TerminalFactory _terminalFactory = null;
    private final static boolean LOCAL_DEBUG = false;
    private final static boolean DEBUG = Debug.DEBUG || LOCAL_DEBUG;

    private static TerminalFactory getSelectedTerminalFactory(String terminalFactoryName, Object params) {
        Security.insertProviderAt(new de.srlabs.simlib.osmocardprovider.OsmoProvider(), 1);

        if (null != _terminalFactory) {
            return _terminalFactory;
        }

        TerminalFactory factory;

        if (null == terminalFactoryName) {
            factory = TerminalFactory.getDefault();
        } else {
            try {
                factory = TerminalFactory.getInstance(terminalFactoryName, params);
            } catch (NoSuchAlgorithmException e) {
                System.out.println(LoggingUtils.formatDebugMessage("TerminalFactory of type " + terminalFactoryName + " does NOT exist."));
                e.printStackTrace(System.err);
                factory = null;
                System.exit(1);
            }
        }

        return factory;
    }

    private ChannelHandler(int readerIndex, String terminalFactoryName) throws CardException {
        _readerIndex = readerIndex;
        _terminalFactoryName = terminalFactoryName;

        TerminalFactory factory = getSelectedTerminalFactory(terminalFactoryName, null);

        List terminals = null;

        try {
            terminals = factory.terminals().list();
        } catch (CardException e) {
            if ("SCARD_E_NO_READERS_AVAILABLE".equals(e.getCause().getMessage())) {
                System.err.println(LoggingUtils.formatDebugMessage("No readers available, please verify that a reader is connected!"));
                System.exit(1);
            }
            throw e;
        }

        if (terminals.isEmpty()) {
            System.err.println(LoggingUtils.formatDebugMessage("No valid PC/SC reader was found, check the connection and pcscd daemon"));
            System.exit(1);
        }

        System.out.println("Terminals: " + terminals);

        if (terminals.size() < (readerIndex + 1)) {
            System.err.println(LoggingUtils.formatDebugMessage("No valid PC/SC reader under index " + readerIndex + ", start from zero!"));
            System.exit(1);
        }

        _cardTerminal = (CardTerminal) terminals.get(readerIndex);
        System.out.println("Using terminal: " + _cardTerminal.getName());
        _card = _cardTerminal.connect("T=0");

        if (null != _card) {
            System.out.println("Card connected: " + _card);
            _cardChannel = _card.getBasicChannel();
            if (null == _cardChannel) {
                System.err.println(LoggingUtils.formatDebugMessage("Unable to open a channel to the card, exiting.."));
                System.exit(1);
            }
        } else {
            System.err.println(LoggingUtils.formatDebugMessage("Unable to connect the card, exiting.."));
            System.exit(1);
        }

    }

    public static synchronized ChannelHandler getInstance(int readerIndex, String terminalFactoryName) throws CardException {
        if (null == _instance) {
            _instance = new ChannelHandler(readerIndex, terminalFactoryName);
            return _instance;
        } else {
            if (readerIndex == _readerIndex && (terminalFactoryName == null ? _terminalFactoryName == null : terminalFactoryName.equals(_terminalFactoryName))) {
                return _instance;
            } else {
                _instance = new ChannelHandler(readerIndex, terminalFactoryName);
                return _instance;
            }
        }
    }

    public static synchronized ChannelHandler getInstance() throws CardException {
        if (null != _instance) {
            return _instance;
        }
        throw new IllegalStateException("Illegal state! There's no initialized channel to the card! Report this bug");
    }

    public static synchronized CardChannel getDefaultChannel() throws CardException {
        if (null == _instance) {
            return getInstance()._cardChannel;
        } else {
            return _instance._cardChannel;
        }

    }

    public static synchronized ResponseAPDU transmitOnDefaultChannel(CommandAPDU apdu) throws CardException {
        return transmitOnDefaultChannel(apdu, true);
    }

    public static synchronized ResponseAPDU transmitOnDefaultChannel(CommandAPDU apdu, boolean retry) throws CardException {
        ResponseAPDU response;

        try {
            response = getDefaultChannel().transmit(apdu);
        } catch (CardException e) {
            if (e.getMessage().contains("SCARD_E_NOT_TRANSACTED")) { // Mac OS X
                System.err.println(LoggingUtils.formatDebugMessage("SCARD_E_NOT_TRANSACTED, trying to reset the card and retry.."));
            } else if (e.getMessage().contains("Unknown error 0x8010002f")) { // Windows (Winscard.h says it's SCARD_E_COMM_DATA_LOST)
                System.err.println(LoggingUtils.formatDebugMessage("Unknown error 0x8010002f, trying to reset the card and retry.."));
            } else if (e.getMessage().contains("Could not obtain response")) {
                System.err.println(LoggingUtils.formatDebugMessage("Could not obtain response detected, this sometimes happens on old cards, trying to reset the card and retry.."));
            } else {
                throw e; // just rethrow the exception as we don't recognize the error
            }
            try {
                Thread.sleep(1000); // sleep for 1 sec so everything has time to settle
            } catch (InterruptedException e2) {
            }
            getInstance().reset(); // reset the card
            System.err.println(LoggingUtils.formatDebugMessage("You might loose context after card reset, only basic TERMINAL PROFILE is executed."));
            AutoTerminalProfile.autoTerminalProfile(); // initialize the card
            if (retry) {
                response = getDefaultChannel().transmit(apdu); // try once again with no catching (the exception will be thrown out of this function if this fails
            } else {
                response = null;
            }
        }

        return response;
    }

    public static void closeChannel() throws CardException {
        if (null != _instance) {
            try {
                getInstance()._cardChannel.close();
                _card.disconnect(true);
            } catch (IllegalStateException e) {
            } // whatever we don't wanna know
        }
    }

    public void reset() throws CardException {
        if (DEBUG) {
            System.out.println(LoggingUtils.formatDebugMessage("Resetting the card.."));
        }
        //_instance._cardChannel.getCard().disconnect(true);
        // documentation says this should be TRUE to reset the card, but there is a bug in Java (reverse logic)
        // https://bugs.openjdk.java.net/show_bug.cgi?id=100151)
        _instance._cardChannel.getCard().disconnect(false);

        _card = _cardTerminal.connect("T=0");
        if (null != _card) {
            _cardChannel = _card.getBasicChannel();
            if (null == _cardChannel) {
                System.err.println(LoggingUtils.formatDebugMessage("Unable to open a channel to the card, exiting.."));
                System.exit(1);
            }
        } else {
            System.err.println(LoggingUtils.formatDebugMessage("Unable to connect the card, exiting.."));
            System.exit(1);
        }
    }

    public String getReaderName() {
        return _cardTerminal.getName();
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException("Clone is not allowed.");
    }
}
