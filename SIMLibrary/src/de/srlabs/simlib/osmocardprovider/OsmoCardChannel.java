package de.srlabs.simlib.osmocardprovider;

import de.srlabs.simlib.Debug;
import de.srlabs.simlib.LoggingUtils;
import java.nio.ByteBuffer;
import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

public class OsmoCardChannel extends CardChannel {

    private final static boolean LOCAL_DEBUG = false;
    private final static boolean DEBUG = Debug.DEBUG || LOCAL_DEBUG;
    private static OsmoCardChannel _instance = null;

    public static synchronized OsmoCardChannel getInstance() {
        if (null == _instance) {
            _instance = new OsmoCardChannel();
            return _instance;
        } else {
            return _instance;
        }
    }

    private OsmoCardChannel() {
    }

    @Override
    public Card getCard() {
        return OsmoCard.getInstance();
    }

    @Override
    public int getChannelNumber() {
        return 0; // we only provide 1 channel always
    }

    @Override
    public ResponseAPDU transmit(CommandAPDU capdu) throws CardException {
        ResponseAPDU response = new ResponseAPDU(OsmoCardTerminal._osmojni.transmit(capdu.getBytes()));
        if (response.getSW() == 0xbaad) {
            System.err.println(LoggingUtils.formatDebugMessage("received 0xbaad SW, SIM failed, trying to power it down/up and reissue last command."));
            OsmoCardTerminal._osmojni.simPowerdown();
            byte[] atr = OsmoCardTerminal._osmojni.simPowerup();
            if (atr.length == 0) {
                System.err.println(LoggingUtils.formatDebugMessage("No SIM present error, please reload your firmware and try again."));
                System.exit(1);
            }
            response = new ResponseAPDU(OsmoCardTerminal._osmojni.transmit(capdu.getBytes()));
            if (response.getSW() == 0xbaad) {
                System.err.println(LoggingUtils.formatDebugMessage("received 0xbaad SW AGAIN, please reload your firmware and try again."));
                System.exit(1);
            }
        }

        return response;
    }

    @Override
    public int transmit(ByteBuffer command, ByteBuffer response) throws CardException {
        throw new CardException("transmit() with ByteBuffers not implemented in OsmoCardChannel");
    }

    @Override
    public void close() throws CardException {
        OsmoCardTerminal._osmojni.simPowerdown(); // closing the channel means power down the sim
        OsmoCard._channelOpened = false;
    }
}
