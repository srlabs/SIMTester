package de.srlabs.simlib;

import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    private static CardChannel _cardChannel;
    private CardTerminal _cardTerminal;
    private static Card _card;
    private final static TerminalFactory _terminalFactory = null;
    private final static boolean LOCAL_DEBUG = false;
    private final static boolean DEBUG = Debug.DEBUG || LOCAL_DEBUG;

    private static boolean trueCardReset = true;

    public static void initReset() {
        Pattern p = Pattern.compile("^(\\d+\\.\\d+\\.\\d+)_?(\\d+)?$");
        Matcher m = p.matcher(System.getProperty("java.version"));

        if (m.find() && m.groupCount() >= 1 && m.groupCount() <= 2) {
            String version;
            int patchversion;

            version = m.group(1);

            patchversion = m.group(2) == null ? 0: Integer.parseInt(m.group(2));

            trueCardReset = (Helpers.versionCompare(version, "1.8.0") > 0 || (Helpers.versionCompare(version, "1.8.0") == 0 && patchversion >= 20)); // Java 1.8.0_20 and above have the correct behavior by default
        } else {
            System.err.println("Unable to detect Java version correctly, setting invertCardReset to TRUE and trying to continue, do not be surprised if card resets won't work!");
            System.setProperty("sun.security.smartcardio.invertCardReset", "true");
            trueCardReset = false;
        }
    }

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
                factory = null; // moronic java
                System.exit(1);
            }
        }

        return factory;
    }

    private ChannelHandler(int readerIndex, String terminalFactoryName) throws CardException {
        initReset();

        _readerIndex = readerIndex;
        _terminalFactoryName = terminalFactoryName;

        TerminalFactory factory = getSelectedTerminalFactory(terminalFactoryName, null);

        List<CardTerminal> terminals = null;

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

        System.out.println("Terminals connected: " + terminals.size());
        for (CardTerminal terminal : terminals) {
            System.out.println(terminal);
        }
        System.out.println();

        if (terminals.size() < (readerIndex + 1)) {
            System.err.println(LoggingUtils.formatDebugMessage("No valid PC/SC reader under index " + readerIndex + ", start from zero!"));
            System.exit(1);
        }

        _cardTerminal = (CardTerminal) terminals.get(readerIndex);
        System.out.println("Using terminal: " + _cardTerminal.getName());

        connectCard();
    }

    private void connectCard() throws CardException {
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
        if (null == getInstance()) {
            throw new IllegalStateException("Illegal state! There's no initialized channel to the card! Report this bug");
        }

        if (_cardChannel == null) {
            getInstance().connectCard();
        }

        return _cardChannel;
    }

    public static synchronized ResponseAPDU transmitOnDefaultChannel(CommandAPDU apdu) throws CardException {
        return transmitOnDefaultChannel(apdu, true);
    }

    public static synchronized ResponseAPDU transmitOnDefaultChannel(CommandAPDU apdu, boolean retry) throws CardException {
        ResponseAPDU response;

        if (DEBUG) {
            System.out.println("TRANSMIT: " + HexToolkit.toString(apdu.getBytes()));
        }

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

        if (DEBUG) {
            if (null != response) {
                System.out.println("RESPONSE: " + HexToolkit.toString(response.getBytes()));
            } else {
                System.out.println("RESPONSE: null");
            }
        }

        return response;
    }

    public static void disconnectCard() throws CardException {
        if (null != _instance) {
            final ExecutorService executor = Executors.newSingleThreadExecutor();
            final Future future = executor.submit(new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    try {
                        _cardChannel = null;
                        _card.disconnect(true); // timeout is here as this sometimes gets stuck and kill -9 <java> sucks
                        _card = null;
                    } catch (IllegalStateException e) {
                    } // whatever we don't wanna know
                    return null;
                }
            });
            executor.shutdown();
            try {
                future.get(1, TimeUnit.SECONDS);
            } catch (TimeoutException | ExecutionException | InterruptedException e) { // if it doesn't work we don't care
            }
        }
    }

    public void reset() throws CardException {
        if (DEBUG) {
            System.out.println(LoggingUtils.formatDebugMessage("Resetting the card.."));
        }
        //_instance._cardChannel.getCard().disconnect(true);
        // documentation says this should be TRUE to reset the card, but there is a bug in Java (reverse logic), nobody knows when and if it will ever get fixed (https://bugs.openjdk.java.net/show_bug.cgi?id=100151), 2012-04-01, the bug is still present in 1.6.0_31
        /* recent development is this says (https://bugs.openjdk.java.net/browse/JDK-8050495#comment-13559746)
         * invertCardReset=true => This property is set by default for 7u72 and later JDK 7 Updates. By default, no behavioral change will be noticed in this area for JDK 7 Update releases.
         * invertCardReset=false => This is default for 8u20 and later JDK 8 Updates.
         */

        _cardChannel.getCard().disconnect(trueCardReset);

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
