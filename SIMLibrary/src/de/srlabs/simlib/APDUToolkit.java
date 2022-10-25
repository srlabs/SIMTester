package de.srlabs.simlib;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
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

        CommandAPDU fetchAPDU;
        if (SIMLibrary.third_gen_apdu) {
            fetchAPDU = new CommandAPDU((byte) 0x80, (byte) 0x12, (byte) 0x00, (byte) 0x00, bytes);
        } else {
            fetchAPDU = new CommandAPDU((byte) 0xA0, (byte) 0x12, (byte) 0x00, (byte) 0x00, bytes);
        }

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

        CommandAPDU getResponse;
        if (SIMLibrary.third_gen_apdu) {
            getResponse = new CommandAPDU(new byte[]{(byte) 0x00, (byte) 0xC0, (byte) 0x00, (byte) 0x00, (byte) bytes}); // Get Response APDU
        } else {
            getResponse = new CommandAPDU(new byte[]{(byte) 0xA0, (byte) 0xC0, (byte) 0x00, (byte) 0x00, (byte) bytes}); // Get Response APDU
        }

        ResponseAPDU r = ChannelHandler.transmitOnDefaultChannel(getResponse);
        if (DEBUG) {
            System.out.println(LoggingUtils.formatDebugMessage("Got response: " + HexToolkit.toString(r.getBytes())));
        }

        return r;
    }

    public static ResponseAPDU runGSMAlgo2G(byte[] rand) throws CardException, FileNotFoundException {
        FileManagement.selectPath("3F007F20");
        CommandAPDU cmd = new CommandAPDU((byte) 0xA0, (byte) 0x88, (byte) 0x00, (byte) 0x00, rand);
        return ChannelHandler.transmitOnDefaultChannel(cmd);
    }

    public static ResponseAPDU authenticate(boolean gsm, byte[] challenge) throws CardException, FileNotFoundException {
        byte p2 = (byte) 0x80; // Specific reference data (for example, DF specific / application dependent key).
        if (gsm) {
            p2 |= (byte) 0x00; // GSM context
        } else {
            p2 |= (byte) 0x01; // 3G context, requires AUTN data in challenge
        }
        CommandAPDU cmd = new CommandAPDU((byte) 0x00, (byte) 0x88, (byte) 0x00, p2, challenge);
        return ChannelHandler.transmitOnDefaultChannel(cmd);
    }

    public static ResponseAPDU sendStatus() throws CardException, FileNotFoundException {
        CommandAPDU status_apdu;
        if (SIMLibrary.third_gen_apdu) {
            status_apdu = new CommandAPDU((byte) 0x00, (byte) 0xF2, (byte) 0x00, (byte) 0x00, (byte) 0x00);
        } else {
            status_apdu = new CommandAPDU((byte) 0xA0, (byte) 0xF2, (byte) 0x00, (byte) 0x00, (byte) 0x00);
        }

        ResponseAPDU response = ChannelHandler.transmitOnDefaultChannel(status_apdu);

        if (response.getSW1() == 0x67) {
            if (SIMLibrary.third_gen_apdu) {
                status_apdu = new CommandAPDU((byte) 0x00, (byte) 0xF2, (byte) 0x00, (byte) 0x00, (byte) response.getSW2());
            } else {
                status_apdu = new CommandAPDU((byte) 0xA0, (byte) 0xF2, (byte) 0x00, (byte) 0x00, (byte) response.getSW2());
            }

            response = ChannelHandler.transmitOnDefaultChannel(status_apdu);
        }
        return response;
    }

    // FIXME: get rid of the string concatenation here, this method can be implemented MUCH nicer, also has TONS hardcoded stuff in it (like CA parameters for Install for Install, should be parametrized as well!)
    public static byte[] generateAppletInstall(byte type, String packageAid, String instanceAid, String TAR) {

        if (SIMLibrary.third_gen_apdu) {
            System.err.println(LoggingUtils.formatDebugMessage("3G format not yet implemented!"));
        }

        byte[] apdu = new byte[]{(byte) 0x80, (byte) 0xE6, type, (byte) 0x00};
        byte[] data;
        String strdata;
        byte[] strdata_bytes;

        switch (type) {
            case APPLET_INSTALL_LOAD:
                strdata = HexToolkit.toString(packageAid.length() / 2);
                strdata += packageAid;
                //strdata += "00000EEF0CC6020000C8020000C7020000"; // FIXME: ugly hardcoded parameters, should be possible to set these
                strdata += "00 00 06 EF 04 C6020000 00".replace(" ", ""); // FIXME: ugly hardcoded parameters, should be possible to set these
                strdata_bytes = HexToolkit.fromString(strdata);
                data = new byte[apdu.length + 1 + strdata.length() / 2];
                System.arraycopy(apdu, 0, data, 0, apdu.length);
                data[apdu.length] = HexToolkit.fromStringToSingleByte(HexToolkit.toString(strdata_bytes.length)); // a bit messy conversion, there shouldb e a method for int(dec) -> byte(hex) conversion
                System.arraycopy(strdata_bytes, 0, data, apdu.length + 1, strdata_bytes.length);
                break;
            case APPLET_INSTALL_INSTALL_AND_MAKE_SELECTABLE:
                String system_parameters = "C8 02 0000 C7 02 0000".replace(" ", "");
                /*
                 * highest priority level (0x01) for toolkit registration
                 * instead of lowest (0xFF) maximum number of timers = 8 (0x08)
                 */
                // system_parameters += "CA 08 01 00 01 08 0F 01 00 00".replace(" ", ""); // this works just fine with ToolkitApplet with Menu Selection, however turned out to not be working with Install for Install if Instance AID contains TAR
                //
                system_parameters += "CA 06 01 00 FF 00 00 00".replace(" ", "");
                // system_parameters += "CA 07 01 00 FF 00 00 00 00".replace(" ", "");
                // system_parameters += "CA 08 01 00 FF 00 00 00 00 00".replace(" ", "");
                // system_parameters += "CA 09 01 00 FF 00 00 00 00 00 00".replace(" ", "");
                /*
                 * CA09
                 *     01 - Length of Access Domain field
                 *     00 - Access Domain field
                 *     FF - Priority level
                 *     00 - Maximum number of Timers
                 *     00 - Maximum length of Toolkit Menu Item test
                 *     00 - Maximum number of Toolkit Menu entries allowed for this instance
                 *     00 - Maximum number of BIP channels allowed for this instance
                 *     00 - Length of Minimum Security Level field
                 *     00 - Length of TAR field
                 */
                String end = "C9 00".replace(" ", ""); // last 0x00 is Length of Install Token

                String aids = HexToolkit.toString(packageAid.length() / 2);
                aids += packageAid;
                aids += HexToolkit.toString(instanceAid.length() / 2);
                aids += instanceAid;
                if (null != TAR && !TAR.isEmpty() && TAR.length() == 6) { // it has to be 6 as it's hex encoded 3-byte value
                    aids += HexToolkit.toString(instanceAid.length() / 2 + 3); // + 3 because we're adding length of binary encoded value
                    aids += instanceAid + TAR;
                } else {
                    aids += HexToolkit.toString(instanceAid.length() / 2);
                    aids += instanceAid;
                }
                aids += "01 00".replace(" ", "");

                strdata = aids + HexToolkit.toString(2 + system_parameters.length() / 2 + end.length() / 2) /*
                         * tag 0xEF + len
                         */ + "EF";
                strdata += HexToolkit.toString(system_parameters.length() / 2) + system_parameters + end + "00";

                strdata_bytes = HexToolkit.fromString(strdata);
                data = new byte[apdu.length + 1 + strdata.length() / 2];
                System.arraycopy(apdu, 0, data, 0, apdu.length);
                data[apdu.length] = HexToolkit.fromStringToSingleByte(HexToolkit.toString(strdata_bytes.length)); // a bit messy conversion, there shouldb e a method for int(dec) -> byte(hex) conversion
                System.arraycopy(strdata_bytes, 0, data, apdu.length + 1, strdata_bytes.length);
                break;
            default:
                throw new IllegalArgumentException("Unsupported install APDU type: " + type);
        }

        if (DEBUG) {
            System.out.println(LoggingUtils.formatDebugMessage("generated data: " + HexToolkit.toString(data)));
        }

        return data;
    }

    // FIXME: get rid of the string concatenation here, this method can be implemented MUCH nicer
    public static byte[] generateAppletDelete(byte type, String packageAid, String instanceAid, String TAR) {

        if (SIMLibrary.third_gen_apdu) {
            System.err.println(LoggingUtils.formatDebugMessage("3G format not yet implemented!"));
        }

        byte[] apdu = new byte[]{(byte) 0x80, (byte) 0xE4, (byte) 0x00};
        byte[] data;
        String strdata;
        byte[] strdata_bytes;

        switch (type) {
            case APPLET_DELETE_PACKAGE:
                //package HexToolkit.fromString("80 E4 00 00 0C 4F 0A".replace(" ", "") + _applet.getPackageAid());

                strdata = "00"; // P2 - 00h: delete object
                strdata += HexToolkit.toString(packageAid.length() / 2 + 2); // this is Lc
                strdata += "4F"; // AID tag
                strdata += HexToolkit.toString(packageAid.length() / 2); // this is AID length
                strdata += packageAid;

                strdata_bytes = HexToolkit.fromString(strdata);

                data = new byte[apdu.length + strdata_bytes.length];
                System.arraycopy(apdu, 0, data, 0, apdu.length);
                System.arraycopy(strdata_bytes, 0, data, apdu.length, strdata_bytes.length);

                break;
            case APPLET_DELETE_INSTANCE:
                // instance HexToolkit.fromString("80 E4 00 00 0D 4F 0B".replace(" ", "") + _applet.getInstanceAid());

                strdata = "00"; // P2 - 00h: delete object
                if (null != TAR && !TAR.isEmpty() && TAR.length() == 6) { // it has to be 6 as it's hex encoded 3-byte value
                    strdata += HexToolkit.toString(instanceAid.length() / 2 + 2 + 3); // this is Lc
                    strdata += "4F"; // AID tag
                    strdata += HexToolkit.toString(instanceAid.length() / 2 + 3); // this is AID length
                    strdata += instanceAid + TAR;
                } else {
                    strdata += HexToolkit.toString(instanceAid.length() / 2 + 2); // this is Lc
                    strdata += "4F"; // AID tag
                    strdata += HexToolkit.toString(instanceAid.length() / 2); // this is AID length
                    strdata += instanceAid;
                }

                strdata_bytes = HexToolkit.fromString(strdata);

                data = new byte[apdu.length + strdata_bytes.length];
                System.arraycopy(apdu, 0, data, 0, apdu.length);
                System.arraycopy(strdata_bytes, 0, data, apdu.length, strdata_bytes.length);

                break;
            case APPLET_DELETE_PACKAGE_AND_ALL_INSTANCES:

                strdata = "80"; // P2 - 80h: delete object and related object
                strdata += HexToolkit.toString(packageAid.length() / 2 + 2); // this is Lc
                strdata += "4F"; // AID tag
                strdata += HexToolkit.toString(packageAid.length() / 2); // this is AID length
                strdata += packageAid;

                strdata_bytes = HexToolkit.fromString(strdata);

                data = new byte[apdu.length + strdata_bytes.length];
                System.arraycopy(apdu, 0, data, 0, apdu.length);
                System.arraycopy(strdata_bytes, 0, data, apdu.length, strdata_bytes.length);

                break;
            default:
                throw new IllegalArgumentException("Unsupported delete APDU type: " + type);
        }

        if (DEBUG) {
            System.out.println(LoggingUtils.formatDebugMessage("generated data: " + HexToolkit.toString(data)));
        }

        return data;
    }

    public static String[] generateAppletLoad(String capFilePath) throws IOException {

        if (SIMLibrary.third_gen_apdu) {
            System.err.println(LoggingUtils.formatDebugMessage("3G format not yet implemented!"));
        }

        FileInputStream fileinputstream = new FileInputStream(capFilePath);

        int numberBytes = fileinputstream.available();
        if (DEBUG) {
            System.out.println(LoggingUtils.formatDebugMessage("reading " + numberBytes + " bytes from " + capFilePath));
        }

        byte cap_data[] = new byte[numberBytes];

        fileinputstream.read(cap_data);
        fileinputstream.close();

        if (DEBUG) {
            System.out.println(LoggingUtils.formatDebugMessage("file read: " + HexToolkit.toString(cap_data)));
        }

        int one_part = 60; // FIXME: this should probably be calculated dynamically, however just to be safe, all the UD headers and APDU start + command packet structure should fit into 30 bytes out of 160
        int parts = (numberBytes + one_part - 1) / one_part; // a bit of black magic to round up the division

        if (DEBUG) {
            System.out.println(LoggingUtils.formatDebugMessage("total bytes: " + numberBytes + "; one_part: " + one_part + "; parts: " + parts));
        }

        String[] output = new String[parts];

        int used_data = 0;

        byte apdu[];

        for (int i = 0; i < parts; i++) {

            // we have to handle this because of the last packet length (shorter)
            if (i + 1 != parts) { // not a last packet
                apdu = new byte[one_part + 5]; // 5 bytes for the CLA, INS, P1, P2, Lc
            } else { // last packet
                apdu = new byte[numberBytes - used_data + 5];
            }

            apdu[0] = (byte) 0x80; // CLA
            apdu[1] = (byte) 0xE8; // INS
            if (i + 1 != parts) { // P1
                apdu[2] = (byte) 0x00; // More blocks
            } else {
                apdu[2] = (byte) 0x80; // Last block
            }
            apdu[3] = HexToolkit.fromStringToSingleByte(HexToolkit.toString(i)); // P2 - Block number starting from 00h.
            apdu[4] = HexToolkit.fromStringToSingleByte(HexToolkit.toString(apdu.length - 5)); // Lc

            if (i == 0) { // length of data is only included in the first packet
                if (cap_data.length >= 0 && cap_data.length <= 127) {
                    apdu[5] = (byte) 0xC4;
                    apdu[6] = (byte) cap_data.length;
                    System.arraycopy(cap_data, used_data, apdu, 7, one_part - 2);
                    used_data += one_part - 2;
                } else if (cap_data.length >= 128 && cap_data.length <= 255) {
                    apdu[5] = (byte) 0xC4;
                    apdu[6] = (byte) 0x81;
                    apdu[7] = (byte) cap_data.length;
                    System.arraycopy(cap_data, used_data, apdu, 8, one_part - 3);
                    used_data += one_part - 3;
                } else if (cap_data.length >= 256 && cap_data.length <= 65535) {
                    apdu[5] = (byte) 0xC4;
                    apdu[6] = (byte) 0x82;
                    apdu[7] = (byte) (cap_data.length >> 8);
                    apdu[8] = (byte) cap_data.length;
                    System.arraycopy(cap_data, used_data, apdu, 9, one_part - 4);
                    used_data += one_part - 4;
                }
            } else if (i + 1 == parts) { // last packet
                System.arraycopy(cap_data, used_data, apdu, 5, numberBytes - used_data);
                used_data += numberBytes - used_data;
                //apdu[apdu.length-1] = (byte) 0x00;
            } else {
                System.arraycopy(cap_data, used_data, apdu, 5, one_part);
                used_data += one_part;
            }

            if (DEBUG) {
                System.out.println(LoggingUtils.formatDebugMessage("data generated: " + HexToolkit.toString(apdu)));
            }
            output[i] = HexToolkit.toString(apdu);
        }

        return output;
    }
}
