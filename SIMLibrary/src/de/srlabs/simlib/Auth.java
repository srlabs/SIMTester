package de.srlabs.simlib;

import java.io.FileNotFoundException;
import java.util.Arrays;
import javax.smartcardio.CardException;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

public class Auth {

    private final static boolean LOCAL_DEBUG = false;
    private final static boolean DEBUG = Debug.DEBUG || LOCAL_DEBUG;

    public static boolean isCHV1Enabled() throws CardException {

        if (SIMLibrary.third_gen_apdu) {
            System.err.println(LoggingUtils.formatDebugMessage("3G format not yet implemented!"));
        }

        SimCardFile file;

        try {
            file = FileManagement.selectPath("3f00");
        } catch (FileNotFoundException e) {
            file = null;
        }

        if (null != file) {
            return !HexToolkit.isBitSet(file.getRawSelectResponseData()[13], 8);
        } else {
            throw new CardException("Unable to read file 3F00, wtf?");
        }
    }

    // A0 20 00 01 08 31 32 33 34 FF FF FF FF (PIN1: 1234)
    public static boolean verifyCHV(int offset, String pin) throws CardException {
        if (DEBUG) {
            System.out.println();
            System.out.println(LoggingUtils.formatDebugMessage("verifyCHV: Verifying PIN/CHV; offset = " + offset + "; key/pin = " + pin));
        }

        byte[] pinData = new byte[8];
        Arrays.fill(pinData, (byte) 0xFF);

        for (int i = 0; i < pin.length(); i++) {
            char c = pin.charAt(i);
            pinData[i] = (byte) c;
        }

        CommandAPDU verifyCHV;
        if (SIMLibrary.third_gen_apdu) {
            verifyCHV = new CommandAPDU((byte) 0x00, (byte) 0x20, (byte) 0x00, (byte) offset, pinData);
        } else {
            verifyCHV = new CommandAPDU((byte) 0xA0, (byte) 0x20, (byte) 0x00, (byte) offset, pinData);
        }

        ResponseAPDU response = ChannelHandler.transmitOnDefaultChannel(verifyCHV);

        switch ((short) response.getSW()) {
            case (short) 0x9000:
                if (DEBUG) {
                    System.out.println(LoggingUtils.formatDebugMessage("verifyCHV: verification successful"));
                }
                return true;
            case (short) 0x9804:
                throw new CardException("verifyCHV: Unsuccessful CHV verification at least one attempt left");
            case (short) 0x9840:
                throw new CardException("verifyCHV: PIN/CHV verification is unsuccessful, no further verification attempt allowed (PIN/CHV is BLOCKED)");
            case (short) 0x9808:
            case (short) 0x6984:
                System.out.println("verifyCHV: PIN/CHV is disabled, we don't need to authenticate on this card");
                return true;
            default:
                throw new CardException("verifyCHV: something not expected has happened, SW = " + Integer.toHexString(response.getSW()));
        }
    }

    public static boolean enableCHV1(String pin) throws CardException {
        if (DEBUG) {
            System.out.println(LoggingUtils.formatDebugMessage("enableCHV1: Enable PIN1/CHV1; key/pin = " + pin));
        }

        byte[] pinData = new byte[8];
        Arrays.fill(pinData, (byte) 0xFF);

        for (int i = 0; i < pin.length(); i++) {
            char c = pin.charAt(i);
            pinData[i] = (byte) c;
        }

        CommandAPDU enableCHV1;
        if (SIMLibrary.third_gen_apdu) {
            enableCHV1 = new CommandAPDU((byte) 0x00, (byte) 0x28, (byte) 0x00, (byte) 0x01, pinData);
        } else {
            enableCHV1 = new CommandAPDU((byte) 0xA0, (byte) 0x28, (byte) 0x00, (byte) 0x01, pinData);
        }

        ResponseAPDU response = ChannelHandler.transmitOnDefaultChannel(enableCHV1);

        switch ((short) response.getSW()) {
            case (short) 0x9000:
                if (DEBUG) {
                    System.out.println(LoggingUtils.formatDebugMessage("enableCHV1: verification successful"));
                }
                return true;
            case (short) 0x9804:
                throw new CardException("enableCHV1: Unsuccessful CHV verification at least one attempt left");
            case (short) 0x9840:
                throw new CardException("enableCHV1: PIN/CHV verification is unsuccessful, no further verification attempt allowed (PIN/CHV is BLOCKED)");
            case (short) 0x9808:
                System.out.println("enableCHV1: PIN/CHV is already disabled, we can't disable it again");
                return true;
            default:
                throw new CardException("enableCHV1: something not expected has happened, SW = " + Integer.toHexString(response.getSW()));
        }

    }

    public static boolean disableCHV(int offset, String pin) throws CardException {
        if (DEBUG) {
            System.out.println(LoggingUtils.formatDebugMessage("disableCHV: Disabling PIN/CHV; offset = " + offset + "; key/pin = " + pin));
        }

        byte[] pinData = new byte[8];
        Arrays.fill(pinData, (byte) 0xFF);

        for (int i = 0; i < pin.length(); i++) {
            char c = pin.charAt(i);
            pinData[i] = (byte) c;
        }

        CommandAPDU disableCHV;
        if (SIMLibrary.third_gen_apdu) {
            disableCHV = new CommandAPDU((byte) 0x00, (byte) 0x26, (byte) 0x00, (byte) offset, pinData);
        } else {
            disableCHV = new CommandAPDU((byte) 0xA0, (byte) 0x26, (byte) 0x00, (byte) offset, pinData);
        }
        
        ResponseAPDU response = ChannelHandler.transmitOnDefaultChannel(disableCHV);

        switch ((short) response.getSW()) {
            case (short) 0x9000:
                if (DEBUG) {
                    System.out.println(LoggingUtils.formatDebugMessage("disableCHV: verification successful"));
                }
                return true;
            case (short) 0x9804:
                throw new CardException("disableCHV: Unsuccessful CHV verification at least one attempt left");
            case (short) 0x9840:
                throw new CardException("disableCHV: PIN/CHV verification is unsuccessful, no further verification attempt allowed (PIN/CHV is BLOCKED)");
            case (short) 0x9808:
                System.out.println("disableCHV: PIN/CHV is already disabled, we can't disable it again");
                return true;
            default:
                throw new CardException("disableCHV: something not expected has happened, SW = " + Integer.toHexString(response.getSW()));
        }

    }
}
