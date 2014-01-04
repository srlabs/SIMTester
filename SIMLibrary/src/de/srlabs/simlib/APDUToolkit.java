package de.srlabs.simlib;

import java.io.FileNotFoundException;
import javax.smartcardio.CardException;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

public class APDUToolkit {

    private final static boolean LOCAL_DEBUG = false;
    private final static boolean DEBUG = Debug.DEBUG || LOCAL_DEBUG;
    public final static byte APPLET_INSTALL_LOAD = (byte) 0x2;
    public final static byte APPLET_INSTALL_INSTALL = (byte) 0x4;
    public final static byte APPLET_INSTALL_MAKE_SELECTABLE = (byte) 0x8;
    public final static byte APPLET_INSTALL_INSTALL_AND_MAKE_SELECTABLE = (byte) 0xC;
    public final static byte APPLET_DELETE_PACKAGE = (byte) 0x0;
    public final static byte APPLET_DELETE_INSTANCE = (byte) 0x1;
    public final static byte APPLET_DELETE_PACKAGE_AND_ALL_INSTANCES = (byte) 0x2;

    public static ResponseAPDU performFetch(int bytes) throws CardException {
        if (DEBUG) {
            System.out.println(LoggingUtils.formatDebugMessage("Fetching " + bytes + " bytes"));
        }
        CommandAPDU fetchAPDU = new CommandAPDU((byte) 0xA0, (byte) 0x12, (byte) 0x00, (byte) 0x00, bytes);
        ResponseAPDU r = ChannelHandler.transmitOnDefaultChannel(fetchAPDU);
        if (DEBUG) {
            System.out.println(LoggingUtils.formatDebugMessage("Fetched: " + HexToolkit.toString(r.getBytes())));
        }
        return r;
    }

    public static ResponseAPDU getResponse(int bytes) throws CardException {
        if (DEBUG) {
            System.out.println(LoggingUtils.formatDebugMessage("Getting response: " + bytes + " bytes"));
        }
        CommandAPDU getResponse = new CommandAPDU(new byte[]{(byte) 0xA0, (byte) 0xC0, (byte) 0x00, (byte) 0x00, (byte) bytes}); // Get Response APDU
        ResponseAPDU r = ChannelHandler.transmitOnDefaultChannel(getResponse);
        if (DEBUG) {
            System.out.println(LoggingUtils.formatDebugMessage("Got response: " + HexToolkit.toString(r.getBytes())));
        }
        return r;
    }

    public static ResponseAPDU runGSMAlgo(byte[] rand) throws CardException, FileNotFoundException {
        FileManagement.selectPath("3F007F20");
        CommandAPDU cmd = new CommandAPDU((byte) 0xA0, (byte) 0x88, (byte) 0x00, (byte) 0x00, rand);
        return ChannelHandler.transmitOnDefaultChannel(cmd);
    }
    
    public static ResponseAPDU sendStatus() throws CardException, FileNotFoundException {
        CommandAPDU cmd = new CommandAPDU((byte) 0xA0, (byte) 0xF2, (byte) 0x00, (byte) 0x00, (byte) 0x00);
        ResponseAPDU response = ChannelHandler.transmitOnDefaultChannel(cmd);
        if (response.getSW1() == 0x67) {
            cmd = new CommandAPDU((byte) 0xA0, (byte) 0xF2, (byte) 0x00, (byte) 0x00, (byte) response.getSW2());
            response = ChannelHandler.transmitOnDefaultChannel(cmd);
        }
        return response;
    }
}
