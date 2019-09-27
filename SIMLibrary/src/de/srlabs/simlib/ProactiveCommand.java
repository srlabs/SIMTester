package de.srlabs.simlib;

import java.text.ParseException;
import javax.smartcardio.ResponseAPDU;

public class ProactiveCommand {

    private final static boolean LOCAL_DEBUG = false;
    private final static boolean DEBUG = Debug.DEBUG || LOCAL_DEBUG;
    private byte[] bytes;
    private final byte[] command_details = new byte[5];
    private final byte[] device_identities = new byte[4];

    public ProactiveCommand(ResponseAPDU rapdu) throws ParseException {
        this(rapdu.getData());
    }

    public ProactiveCommand(byte[] data) throws ParseException {

        bytes = new byte[data.length];
        System.arraycopy(data, 0, bytes, 0, data.length);

        if (!((byte) 0xD0 == data[0])) { // check if data look like a proactive command based on tag
            throw new ParseException("data don't look like a valid Proactive command", 0);
        }

        if (data.length < (2 + command_details.length + device_identities.length)) {
            throw new ParseException("Not enough data for mandatory fields", 0);
        }

        // handle length
        int offset;

        if ((byte) 0x81 == data[1]) {
            offset = 1;
        } else {
            offset = 2;
        }

        if ((byte) (data.length - offset) != data[1]) {
            throw new ParseException("data don't correspont with length (2nd byte); data dump -> " + HexToolkit.toString(data), 0);
        }

        if (DEBUG) {
            System.out.println(LoggingUtils.formatDebugMessage("Raw ProactiveCommand: " + HexToolkit.toString(data) + " offset: " + offset));
        }

        if ((data[offset] & 0x7F) != 0x01) {
            throw new ParseException("Unable to find COMMAND DETAILS TAG (0x81) at position " + offset + ", data: " + HexToolkit.toString(data), 0);
        }

        System.arraycopy(data, offset, command_details, 0, 5); // copy COMMAND DETAILS

        if (DEBUG) {
            System.out.println(LoggingUtils.formatDebugMessage("Found Command details tag: " + HexToolkit.toString(command_details)));
        }

        if ((data[offset + command_details.length] & 0x7F) != 0x02) {
            throw new ParseException("Unable to find DEVICE IDENTITIES TAG (0x82) at position " + (offset + command_details.length) + ", data: " + HexToolkit.toString(data), 0);
        }

        System.arraycopy(data, offset + command_details.length, device_identities, 0, 4); // copy DEVICE IDENTITIES

        if (DEBUG) {
            System.out.println(LoggingUtils.formatDebugMessage("Found Device identities tag: " + HexToolkit.toString(device_identities)));
        }

        if (DEBUG) {
            System.out.println(LoggingUtils.formatDebugMessage("ProactiveCommand: " + identifyProactiveCommand(command_details)));
        }
    }

    public byte[] getCommandDetails() {
        return command_details;
    }

    public byte[] getDeviceIdentities() {
        return device_identities;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public String getType() {
        return identifyProactiveCommand(command_details);
    }

    public String getSummary() {
        String type = identifyProactiveCommand(command_details);
        String summary = "";

        try {
            switch (type) {
                case "SEND SHORT MESSAGE":
                    byte[] sms_tlv = TLVToolkit.getTLV(bytes, (byte) 0x8B, (byte) 0x81);
                    if (null == sms_tlv) {
                        sms_tlv = TLVToolkit.getTLV(bytes, (byte) 0x0B, (byte) 0x81);
                    }
                    byte[] sms_msg = new byte[sms_tlv.length - 2]; // get rid of TAG+len
                    System.arraycopy(sms_tlv, 2, sms_msg, 0, sms_msg.length);
                    summary = "\"" + HexToolkit.toString(sms_msg) + "\"";
                    break;
                case "DISPLAY TEXT":
                    byte[] dt_tlv = TLVToolkit.getTLV(bytes, (byte) 0x8D, (byte) 0x81);
                    if (null == dt_tlv) {
                        dt_tlv = TLVToolkit.getTLV(bytes, (byte) 0x0D, (byte) 0x81);
                    }
                    byte[] dt_data = new byte[dt_tlv.length - 2];  // get rid of TAG+len
                    System.arraycopy(dt_tlv, 2, dt_data, 0, dt_data.length);
                    byte[] dt_msg = new byte[dt_data.length - 1];  // get encoding byte
                    System.arraycopy(dt_data, 1, dt_msg, 0, dt_msg.length);
                    summary = "\"" + HexToolkit.toText(dt_msg) + "\"";
                    break;
            }

        } catch (Exception e) {
            summary = "___ERROR DECODING PROACTIVE COMMAND___";
        }

        return summary;
    }

    public static String identifyProactiveCommand(byte[] CommandDetails) {
        String name = "NOT IDENTIFIED";

        if (((byte) 0x01 == (CommandDetails[0] & 0x7F)) && CommandDetails.length == 5) {
            switch (CommandDetails[3]) {
                case (byte) 0x03:
                    name = "POLL INTERVAL";
                    break;
                case (byte) 0x05:
                    name = "SET UP EVENT LIST";
                    break;
                case (byte) 0x10:
                    name = "SETUP CALL";
                    break;
                case (byte) 0x11:
                    name = "SEND SS";
                    break;
                case (byte) 0x12:
                    name = "SEND USSD";
                    break;
                case (byte) 0x13:
                    name = "SEND SHORT MESSAGE";
                    break;
                case (byte) 0x20:
                    name = "PLAY TONE";
                    break;
                case (byte) 0x21:
                    name = "DISPLAY TEXT";
                    break;
                case (byte) 0x23:
                    name = "GET INPUT";
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
