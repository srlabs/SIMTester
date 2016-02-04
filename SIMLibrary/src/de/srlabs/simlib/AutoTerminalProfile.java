package de.srlabs.simlib;

import java.text.ParseException;
import javax.smartcardio.CardException;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

public class AutoTerminalProfile {

    private final static boolean LOCAL_DEBUG = false;
    private final static boolean DEBUG = Debug.DEBUG || LOCAL_DEBUG;

    public static boolean autoTerminalProfile() throws CardException {
        ResponseAPDU r;

        if (DEBUG) {
            System.out.println();
            System.out.println(LoggingUtils.formatDebugMessage("Starting automatic Terminal Profile initialization"));
            System.out.println();
        }

        byte[] baCommandAPDU;
        if (SIMLibrary.third_gen_apdu) {
            baCommandAPDU = HexToolkit.fromString("8010000011FF9FFFFFFF0F1FFF7F0300002008200000"); //terminal profile APDU with some pre-specified capabilities
        } else {
            baCommandAPDU = HexToolkit.fromString("A010000011FF9FFFFFFF0F1FFF7F0300002008200000"); //terminal profile APDU with some pre-specified capabilities
        }

        if (DEBUG) {
            System.out.println(LoggingUtils.formatDebugMessage("Sending TERMINAL PROFILE APDU: " + HexToolkit.toString(baCommandAPDU)));
            System.out.println();
        }

        r = ChannelHandler.transmitOnDefaultChannel(new CommandAPDU(baCommandAPDU));

        while ((r.getSW1() != (byte) 0x90 && r.getSW2() != (byte) 0x00) || r.getData().length > 0) {
            r = handleResponse(r);
        }

        return true;
    }

    public static ResponseAPDU handleResponse(ResponseAPDU response) throws CardException {
        ResponseAPDU r;

        if (0x91 == response.getSW1()) {
            r = APDUToolkit.performFetch(response.getSW2());
        } else if (response.getData().length > 0 && (byte) 0xD0 == response.getData()[0]) {
            r = handleProactiveCommand(response);
        } else {
            throw new CardException("There was a problem while doing automatic Terminal Profile; Unidentifiable response was: " + HexToolkit.toString(response.getBytes()));
        }

        return r;
    }

    public static ResponseAPDU handleProactiveCommand(ResponseAPDU response) throws CardException {
        ResponseAPDU r;
        ProactiveCommand pc;

        try {
            pc = new ProactiveCommand(response);
        } catch (ParseException e) {
            //throw new CardException(e);
            System.err.println("Unable to parse ProactiveCommand, sending fake (zero length) TERMINAL RESPONSE, this may get ugly!");

            CommandAPDU tr_apdu;
            if (SIMLibrary.third_gen_apdu) {
                tr_apdu = new CommandAPDU((byte) 0x80, (byte) 0x14, (byte) 0x00, (byte) 0x00, HexToolkit.fromString("81 03 010000 82 02 82 81 83 01 00".replaceAll(" ", "")));
            } else {
                tr_apdu = new CommandAPDU((byte) 0xA0, (byte) 0x14, (byte) 0x00, (byte) 0x00, HexToolkit.fromString("81 03 010000 82 02 82 81 83 01 00".replaceAll(" ", "")));
            }

            r = ChannelHandler.transmitOnDefaultChannel(tr_apdu);
            return r;
        }

        byte[] pc_command_details = pc.getCommandDetails();
        byte[] data = pc.getBytes();

        byte[] tr_device_identities = new byte[]{(byte) 0x82, (byte) 0x02, (byte) 0x82, (byte) 0x81};
        byte[] tr_result_successful = new byte[]{(byte) 0x83, (byte) 0x01, (byte) 0x00};

        byte[] tr_data = new byte[pc_command_details.length + tr_device_identities.length + tr_result_successful.length];

        if ((byte) 0x81 == pc_command_details[0] && pc_command_details.length == 5 && pc_command_details[3] == 0x03) {
            // POLL INTERVAL detected
            byte[] poll_interval = TLVToolkit.getTLV(data, (byte) 0x84);
            if (null == poll_interval) {
                poll_interval = TLVToolkit.getTLV(data, (byte) 0x04);
                if (null == poll_interval) {
                    throw new CardException("handleProactiveCommand: failure during POLL INTERVAL proactive command handling, DEBUG THIS!");
                }
            }
            if (poll_interval.length == 4) {
                tr_data = new byte[pc_command_details.length + tr_device_identities.length + tr_result_successful.length + poll_interval.length];
                System.arraycopy(poll_interval, 0, tr_data, pc_command_details.length + tr_device_identities.length + tr_result_successful.length, poll_interval.length);
            }
        }

        // debug loc info
        if ((byte) 0x81 == pc_command_details[0] && pc_command_details.length == 5 && pc_command_details[3] == 0x26) {
            if (pc_command_details[4] == 0x00) {
                byte[] loc_info = new byte[]{(byte) 0x13, (byte) 0x4, (byte) 0x11, (byte) 0x22, (byte) 0x33, (byte) 0x44};
                tr_data = new byte[pc_command_details.length + tr_device_identities.length + tr_result_successful.length + loc_info.length];
                System.arraycopy(loc_info, 0, tr_data, pc_command_details.length + tr_device_identities.length + tr_result_successful.length, loc_info.length);
            } else if (pc_command_details[4] == 0x01) {
                byte[] imei = new byte[]{(byte) 0x14, (byte) 0x08, (byte) 0x11, (byte) 0x22, (byte) 0x33, (byte) 0x44, (byte) 0x55, (byte) 0x66, (byte) 0x77, (byte) 0x88};
                tr_data = new byte[pc_command_details.length + tr_device_identities.length + tr_result_successful.length + imei.length];
                System.arraycopy(imei, 0, tr_data, pc_command_details.length + tr_device_identities.length + tr_result_successful.length, imei.length);
            }
        }

        System.arraycopy(pc_command_details, 0, tr_data, 0, pc_command_details.length);
        System.arraycopy(tr_device_identities, 0, tr_data, pc_command_details.length, tr_device_identities.length);
        System.arraycopy(tr_result_successful, 0, tr_data, pc_command_details.length + tr_device_identities.length, tr_result_successful.length);

        CommandAPDU tr_apdu;
        if (SIMLibrary.third_gen_apdu) {
            tr_apdu = new CommandAPDU((byte) 0x80, (byte) 0x14, (byte) 0x00, (byte) 0x00, tr_data);
        } else {
            tr_apdu = new CommandAPDU((byte) 0xA0, (byte) 0x14, (byte) 0x00, (byte) 0x00, tr_data);
        }

        if (DEBUG) {
            System.out.println(LoggingUtils.formatDebugMessage("handleProactiveCommand: terminal response complete APDU: " + HexToolkit.toString(tr_apdu.getBytes())));
            System.out.println();
        }

        r = ChannelHandler.transmitOnDefaultChannel(tr_apdu);

        return r;
    }
}
