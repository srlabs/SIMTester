package de.srlabs.simlib;

import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

public class AutoTerminalProfile {

    private final static boolean LOCAL_DEBUG = false;
    private final static boolean DEBUG = Debug.DEBUG || LOCAL_DEBUG;
    private static CardChannel _channel;

    public static boolean autoTerminalProfile() throws CardException {
        ResponseAPDU r;

        if (DEBUG) {
            System.out.println();
            System.out.println(LoggingUtils.formatDebugMessage("Starting automatic Terminal Profile initialization"));
            System.out.println();
        }

        byte[] baCommandAPDU = HexToolkit.fromString("A010000011FF9FFFFFFF0F1FFF7F0300002008200000"); //terminal profile APDU with some pre-specified capabilities
        if (DEBUG) {
            System.out.println(LoggingUtils.formatDebugMessage("Sending TERMINAL PROFILE APDU: " + HexToolkit.toString(baCommandAPDU)));
            System.out.println();
        }

        _channel = ChannelHandler.getDefaultChannel();

        r = _channel.transmit(new CommandAPDU(baCommandAPDU));

        while ((r.getSW1() != 90 && r.getSW2() != 0x00) || r.getData().length > 0) {
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

        byte[] data = response.getData();

        if (!((byte) 0xD0 == data[0])) { // check if data look like a proactive command based on tag
            throw new CardException("handleProactiveCommand: data in ResponseAPDU don't look like a valid Proactive command");
        }

        // handle length
        int offset;

        if ((byte) 0x81 == data[1]) {
            offset = 1;
        } else {
            offset = 0;
        }


        if ((byte) (data.length - 2 - offset) != data[1 + offset]) {
            throw new CardException("handleProactiveCommand: data in ResponseAPDU don't correspont with length (2nd byte); data dump -> " + HexToolkit.toString(data));
        }

        byte[] pc_command_details = new byte[5]; // Proactive command; command details
        System.arraycopy(data, 2 + offset, pc_command_details, 0, 5);

        if (DEBUG) {
            System.out.println(LoggingUtils.formatDebugMessage("Raw ProactiveCommand: " + HexToolkit.toString(data)));
        }

        if (DEBUG) {
            System.out.println(LoggingUtils.formatDebugMessage("ProactiveCommand: " + identifyProactiveCommand(pc_command_details) + " proactive command, trying to format Terminal Reponse"));
        }

        byte[] tr_device_identities = new byte[]{(byte) 0x82, (byte) 0x02, (byte) 0x82, (byte) 0x81};
        byte[] tr_result_successful = new byte[]{(byte) 0x83, (byte) 0x01, (byte) 0x00};

        byte[] tr_data = new byte[pc_command_details.length + tr_device_identities.length + tr_result_successful.length];

        if ((byte) 0x81 == pc_command_details[0] && pc_command_details.length == 5 && pc_command_details[3] == 0x03) {
            // POLL INTERVAL detected
            byte[] poll_interval = TLVToolkit.getTLV(data, 0x84);
            if (null == poll_interval) {
                poll_interval = TLVToolkit.getTLV(data, 0x04);
                if (null == poll_interval) {
                    throw new CardException("handleProactiveCommand: failure during POLL INTERVAL proactive command handling, DEBUG THIS!");
                }
            }
            if (poll_interval.length == 4) {
                tr_data = new byte[pc_command_details.length + tr_device_identities.length + tr_result_successful.length + poll_interval.length];
                System.arraycopy(poll_interval, 0, tr_data, pc_command_details.length + tr_device_identities.length + tr_result_successful.length, poll_interval.length);
            }
        }

        System.arraycopy(pc_command_details, 0, tr_data, 0, pc_command_details.length);
        System.arraycopy(tr_device_identities, 0, tr_data, pc_command_details.length, tr_device_identities.length);
        System.arraycopy(tr_result_successful, 0, tr_data, pc_command_details.length + tr_device_identities.length, tr_result_successful.length);

        CommandAPDU tr_apdu = new CommandAPDU((byte) 0xA0, (byte) 0x14, (byte) 0x00, (byte) 0x00, tr_data);

        if (DEBUG) {
            System.out.println(LoggingUtils.formatDebugMessage("handleProactiveCommand: terminal response complete APDU: " + HexToolkit.toString(tr_apdu.getBytes())));
            System.out.println();
        }

        r = _channel.transmit(tr_apdu);

        return r;
    }

    public static String identifyProactiveCommand(byte[] CommandDetails) {
        String name = "NOT IDENTIFIED";

        if ((byte) 0x81 == CommandDetails[0] && CommandDetails.length == 5) {
            switch (CommandDetails[3]) {
                case (byte) 0x03:
                    name = "POLL INTERVAL";
                    break;
                case (byte) 0x05:
                    name = "SET UP EVENT LIST";
                    break;
                case (byte) 0x13:
                    name = "SEND SHORT MESSAGE";
                    break;
                case (byte) 0x21:
                    name = "DISPLAY TEXT";
                    break;
                case (byte) 0x25:
                    name = "SET UP MENU";
                    break;
                case (byte) 0x26:
                    name = "PROVIDE LOCAL INFORMATION";
                    break;
                default:
                    name = "NOT IDENTIFIED (" + HexToolkit.toString(CommandDetails[3]) + ")";
            }
        }

        return name;
    }
}
