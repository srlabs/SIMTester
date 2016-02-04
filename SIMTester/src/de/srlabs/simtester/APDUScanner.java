package de.srlabs.simtester;

import de.srlabs.simlib.APDUToolkit;
import de.srlabs.simlib.AutoTerminalProfile;
import de.srlabs.simlib.ChannelHandler;
import de.srlabs.simlib.CommandPacket;
import de.srlabs.simlib.HexToolkit;
import de.srlabs.simlib.LoggingUtils;
import de.srlabs.simlib.OTASMS;
import de.srlabs.simlib.ResponsePacket;
import de.srlabs.simlib.SIMLibrary;
import java.text.ParseException;
import javax.smartcardio.CardException;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

public class APDUScanner {

    public static void run(EntryPoint ep, CSVWriter writer, boolean viaOTA, boolean level2) throws CardException {

        System.out.println();
        if (ep != null)
            System.out.println("Performing a " + (level2 ? "LEVEL 2" : "LEVEL 1") + " APDU scan" + (!level2 ? " for TAR " + HexToolkit.toString(ep.getTAR()) + "." : "."));
        else
            System.out.println("Performing a " + (level2 ? "LEVEL 2" : "LEVEL 1") + " APDU scan.");
        
        System.out.println();

        CommandPacket cp = (viaOTA ? ep.getCommandPacket() : new CommandPacket());
        OTASMS otasms = new OTASMS();

        for (short cla = 0; cla <= (short) 0xff && !Thread.currentThread().isInterrupted(); cla++) {
            short cla_SW;
            byte[] response_data = null;
            if (viaOTA) {
                byte[] cla_apdu = new byte[]{(byte) cla, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00};
                cp.setUserData(cla_apdu);
                otasms.setCommandPacket(cp);
                ResponseAPDU cla_res = otasms.send();
                response_data = getResponseData(cla_res);

                if (!level2) {
                    writer.writeLine(HexToolkit.toString(ep.getTAR()), cp.getBytes(), response_data);
                }

                if (null == response_data) {
                    continue;
                }

                cla_SW = getSWFromResponseData(response_data);

                if (cla_SW == 0xbaad) {
                    continue;
                }
            } else {
                CommandAPDU cla_apdu = new CommandAPDU((byte) cla, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00);
                ResponseAPDU cla_res = ChannelHandler.transmitOnDefaultChannel(cla_apdu);

                if (!level2) {
                    writer.writeLine("I/O", cla_apdu.getBytes(), cla_res.getBytes());
                }

                cla_SW = (short) cla_res.getSW();
            }

            if (cla_SW == 0x6E00 || cla_SW == 0x6881 || cla_SW == 0x6882) {
                continue;
            }

            System.out.println("Valid CLA found: " + HexToolkit.toString((byte) cla) + ", response was: 0x" + String.format("%04X", cla_SW) + ", response_data: " + HexToolkit.toString(response_data));

            if (level2) {
                for (short ins = 0; ins <= (short) 0xff && !Thread.currentThread().isInterrupted(); ins++) {
                    short ins_SW;
                    ResponseAPDU ins_res = null;
                    response_data = null;
                    if (viaOTA) {
                        byte[] ins_apdu = new byte[]{(byte) cla, (byte) ins, (byte) 0x00, (byte) 0x00, (byte) 0x00};
                        cp.setUserData(ins_apdu);
                        otasms.setCommandPacket(cp);
                        ins_res = otasms.send();
                        response_data = getResponseData(ins_res);
                        writer.writeLine(HexToolkit.toString(ep.getTAR()), cp.getBytes(), response_data);
                        if (null == response_data) {
                            continue;
                        }
                        ins_SW = getSWFromResponseData(response_data);

                        if (ins_SW == 0xbaad) {
                            continue;
                        }
                    } else {
                        CommandAPDU ins_apdu = new CommandAPDU((byte) cla, (byte) ins, (byte) 0x00, (byte) 0x00, (byte) 0x00);
                        try {
                            ins_res = ChannelHandler.transmitOnDefaultChannel(ins_apdu);
                        } catch (IllegalArgumentException e) {
                            if ("Manage channel command not allowed, use openLogicalChannel()".equals(e.getMessage())) {
                                continue;
                            }
                        } catch (CardException e) {
                            writer.writeLine("I/O", ins_apdu.getBytes(), new byte[]{});
                            continue;
                        }
                        writer.writeLine("I/O", ins_apdu.getBytes(), ins_res.getBytes());
                    }

                    if (null == ins_res) {
                        System.err.println(LoggingUtils.formatDebugMessage("ins_res is null -> This should never happened, report a bug!"));
                        continue;
                    }

                    ins_SW = (short) ins_res.getSW();

                    if (ins_SW == 0x6D00) {
                        continue;
                    }

                    System.out.println("Valid APDU found: CLA(" + HexToolkit.toString((byte) cla) + "), INS(" + HexToolkit.toString((byte) ins) + "), response: 0x" + String.format("%04X", ins_SW) + ", response_data: " + HexToolkit.toString(response_data));
                }
            }
        }
        System.out.println();
        if (ep != null)
            System.out.println("APDU scan has finished" + (!level2 ? " on TAR " + HexToolkit.toString(ep.getTAR()) + "." : "."));
        else
            System.out.println("APDU scan has finished.");
    }

    private static byte[] getResponseData(ResponseAPDU response) throws CardException {
        byte[] response_data = null;

        if ((byte) response.getSW1() == (byte) 0x9E || (byte) response.getSW1() == (byte) 0x9F
                || (SIMLibrary.third_gen_apdu && ((byte) response.getSW1() == (byte) 0x62 || (byte) response.getSW1() == (byte) 0x61))) {
            response_data = APDUToolkit.getResponse(response.getSW2()).getData();
        } else if ((byte) response.getSW1() == (byte) 0x91) { // fetch
            ResponseAPDU fetch_response = APDUToolkit.performFetch(response.getSW2());
            byte[] fetched_data = fetch_response.getData();
            System.out.println("card responded with FETCH, fetched_data = " + HexToolkit.toString(fetched_data));

            if (fetched_data[0] == (byte) 0xD0) { // handling of proactive data..
                System.out.println("first byte of fetch data is 0xD0, trying to handle the proactive command we fetched.. ");
                AutoTerminalProfile.handleProactiveCommand(fetch_response);
            }

            response_data = ResponsePacket.Helpers.findResponsePacket(fetched_data);
            if (null == response_data) {
                System.out.println("Unable to locate Response Packet Header in fetched data, skipping..");
                return null;
            }
        }
        return response_data;
    }

    private static short getSWFromResponseData(byte[] response_data) {

        if (null != response_data) {
            ResponsePacket rp = new ResponsePacket();

            try {
                rp.parse(response_data);
            } catch (ParseException e) {
                System.err.println("Parse exception while parsing response packet, skipping it! details:");
                e.printStackTrace(System.err);
                return (short) 0xbaad;
            }

            byte status_code = rp.getStatusCode();
            if (status_code == 0) {
                if (rp.areAdditionalDataPresent()) {
                    byte[] additionalData = rp.getAdditionalData();
                    if (additionalData.length == 3) {
                        short SW = (short) ((additionalData[1] << 8) | (additionalData[2] & 0xFF));
                        return SW;
                    } else {
                        return (short) 0xbaad;
                    }
                } else {
                    return (short) 0xbaad;
                }
            } else {
                return (short) 0xbaad;
            }
        } else {
            return (short) 0xbaad;
        }
    }
}
