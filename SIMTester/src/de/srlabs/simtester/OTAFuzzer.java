package de.srlabs.simtester;

import de.srlabs.simlib.AutoTerminalProfile;
import de.srlabs.simlib.CommandPacket;
import de.srlabs.simlib.Helpers;
import de.srlabs.simlib.HexToolkit;
import de.srlabs.simlib.LoggingUtils;
import de.srlabs.simlib.OTASMS;
import de.srlabs.simlib.SMSDeliverTPDU;
import de.srlabs.simlib.ResponsePacket;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import javax.smartcardio.CardException;
import javax.smartcardio.ResponseAPDU;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class OTAFuzzer {

    private final static boolean LOCAL_DEBUG = false;
    private final static boolean DEBUG = SIMTester.DEBUG || LOCAL_DEBUG;
    private final static int CPH_fuzz_count = 2;


    public static byte[] longToBytes(long x) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(x);
        return buffer.array();
    }




    public static void fuzzOTA(int keyset, String TAR, FuzzerData fuzzer, CSVWriter writer, boolean bruteforce) throws CardException {


        ArrayList<FuzzerResult> signedResponses = new ArrayList<FuzzerResult>();
        ArrayList<FuzzerResult> encryptedResponses = new ArrayList<FuzzerResult>();


        System.out.println();
        System.out.println("Starting OTA passthrough fuzzing (PID, DCS, UDHI, IEI/CPH)");
        System.out.println();
        System.out.println("Using the following values in packets: TAR = " + TAR + ", keyset = " + keyset + ", fuzzer = " + fuzzer._name);
        System.out.println();

        if (AutoTerminalProfile.autoTerminalProfile()) {
            if (DEBUG) {
                System.out.println(LoggingUtils.formatDebugMessage("Automatic Terminal profile initialization SUCCESSFUL!"));
            }
        } else {
            if (DEBUG) {
                System.out.println(LoggingUtils.formatDebugMessage("Automatic Terminal profile initialization FAILED!"));
            }
        }

        byte[] pid_values = new byte[256];
        byte[] dcs_values = new byte[256];

        if (bruteforce) {
            for (int i = 0; i <= 0xFF; i++) {
                pid_values[i] = (byte) i;
                dcs_values[i] = (byte) i;
            }
        } else {
            pid_values = new byte[]{(byte) 0, (byte) 65, (byte) 124, (byte) 127};
            dcs_values = new byte[]{(byte) 0, (byte) 22, (byte) 54, (byte) 86, (byte) 118, (byte) 150, (byte) 182, (byte) 214, (byte) 246};
        }

        List<String> CPH_values = new ArrayList();
        boolean[] udhi_values = new boolean[]{false, true};

        /* standard security header for OTA command */
        for (int i = 0; i <= CPH_fuzz_count; i++) {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("%02X%02X%02X", 2 + i, 0x70, i));
            for (int j = 0; j < i; j++) {
                sb.append(String.format("%02X", 0));
            }
            CPH_values.add(sb.toString());
        }

        /* standard security header for OTA response */
        CPH_values.add("027100");
        /* non-standard security header */
        CPH_values.add("027F00");
        /* test also with no security header */
        CPH_values.add("");
        
        int loopUntil = pid_values.length * dcs_values.length * CPH_values.size() * udhi_values.length;
        long startTime = System.currentTimeMillis();
        int loop = 0;

        System.out.println("This scan will go over " + loopUntil + " PID, DCS, UDHI, IEI/CPH.");
        System.out.println();

        writer.writeRawLine("# pid,dcs,udhi,cph,response");
        writer.writeRawLine("# msgs: " + loopUntil);

        /* do the fuzzing */
        for (byte pid : pid_values) {
            for (byte dcs : dcs_values) {
                for (String CPH : CPH_values) {
                    for (boolean udhi : udhi_values) {

                        if ((loop == loopUntil) || (loop % 100) == 0) {
                            long currentTime = System.currentTimeMillis();
                            if (loop != 0) {
                                long timePassed = currentTime - startTime;
                                long diffTime = (long) (((double) timePassed / (double) loop) * (loopUntil - loop));
                                Duration duration;
                                try {
                                    duration = DatatypeFactory.newInstance().newDuration(diffTime);
                                    System.out.printf("already processed: %d msgs, to process: %d msgs, approximate remaining time: %d days, %d hours, %d minutes, %d seconds\n", loop, (loopUntil - loop), duration.getDays(), duration.getHours(), duration.getMinutes(), duration.getSeconds());
                                } catch (DatatypeConfigurationException ex) {
                                }
                            }
                        }

                        CommandPacket cp = Fuzzer.generateCommandPacket(keyset, fuzzer._counter, fuzzer._kic, fuzzer._kid, TAR, fuzzer._requestPoR, fuzzer._cipherPoR);
                        SMSDeliverTPDU tpdu = new SMSDeliverTPDU();

                        tpdu.setPID((byte) pid);
                        tpdu.setDCS((byte) dcs);
                        tpdu.setTPUDHI(udhi);
                        cp.setCPH(HexToolkit.fromString(CPH));
                        tpdu.setTPUD(cp.getBytes());

                        OTASMS sms = new OTASMS();
                        sms.setSMSDeliverTPDU(tpdu);
                        ResponseAPDU response = sms.send();

                        ResponseAPDU handled_response = Helpers.handleSIMResponse(response, false);
                        ResponsePacket responsePacket = new ResponsePacket();


                        // response is not 2 bytes long and 9000 (those are the non-responsive combinations, we only display the interesting (responsive) ones)
                        if (handled_response.getBytes().length > 2) {
                            String output = "Message "
                                    + "PID = " + String.format("%02X", pid)
                                    + ", DCS = " + String.format("%02X", dcs)
                                    + ", UDHI = " + (udhi ? "1" : "0")
                                    + ", CPH = " + String.format("%-" + Integer.toString((1 + CPH_fuzz_count + 2) * 2) + "s", CPH)
                                    + " -> response: " + HexToolkit.toString(handled_response.getBytes());
                            
                            // handle Proactive SIM requests, if any
                            try {
                                byte[] response_data = null;
                                
                                response_data = Fuzzer.getOTAResponse(handled_response, cp, writer, fuzzer, TAR, keyset);
                            } catch (Exception e) {
                                System.err.println("Parsing of responsePacket data failed: "+HexToolkit.toString(handled_response.getBytes())+"\n with exception: "+e.getMessage());
                            }

                            System.out.println(output);

                            // pid,dcs,udhi,cph/iei,response
                            writer.writeRawLine(String.format("%02X", pid) + ","
                                    + String.format("%02X", dcs) + ","
                                    + (udhi ? "1" : "0") + ","
                                    + CPH + ","
                                    + HexToolkit.toString(handled_response.getBytes()));
                            }
                        loop++;
                    }
                }
            }
        }

    }

}
