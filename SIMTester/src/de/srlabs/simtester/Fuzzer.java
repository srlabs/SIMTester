package de.srlabs.simtester;

import de.srlabs.simlib.APDUToolkit;
import de.srlabs.simlib.Address;
import de.srlabs.simlib.AutoTerminalProfile;
import de.srlabs.simlib.ChannelHandler;
import de.srlabs.simlib.CommandPacket;
import de.srlabs.simlib.EnvelopeSMSPPDownload;
import de.srlabs.simlib.Helpers;
import de.srlabs.simlib.HexToolkit;
import de.srlabs.simlib.LoggingUtils;
import de.srlabs.simlib.ProactiveCommand;
import de.srlabs.simlib.ResponsePacket;
import de.srlabs.simlib.SIMLibrary;
import de.srlabs.simlib.SMSDeliverTPDU;
import de.srlabs.simlib.SMSTPDU;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import javax.smartcardio.CardException;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

public class Fuzzer extends Thread {

    private final static boolean LOCAL_DEBUG = false;
    private final static boolean DEBUG = SIMTester.DEBUG || LOCAL_DEBUG;
    private static boolean scannedTARwithPoR09 = false;
    private static boolean skipTAROnPoR09 = false;
    public static Byte KIC = null;
    public static Byte KID = null;
    public static Byte SPI1 = null;
    public static Byte SPI2 = null;
    public static boolean use_sms_submit = true;
    private CSVWriter _writer;
    private List<String> _TARs = null;
    private List _keysets = null;
    private ArrayList<FuzzerData> _fuzzers = null;
    public ArrayList<FuzzerResult> signedResponses = new ArrayList<FuzzerResult>();
    public ArrayList<FuzzerResult> encryptedResponses = new ArrayList<FuzzerResult>();
    public ArrayList<FuzzerResult> unprotectedTARsResponses = new ArrayList<FuzzerResult>();
    public ArrayList<FuzzerResult> decryptionOracleResponses = new ArrayList<FuzzerResult>();
    public ArrayList<FuzzerResult> wibCommandExecuted = new ArrayList<FuzzerResult>();
    public ArrayList<FuzzerResult> satCommandExecuted = new ArrayList<FuzzerResult>();

    public Fuzzer(CSVWriter writer, List<String> TARs, List keysets, ArrayList<FuzzerData> fuzzers) {
        if (null == writer) {
            throw new IllegalArgumentException("writer cannot be null!");
        }
        if (null == TARs) {
            throw new IllegalArgumentException("TARs cannot be null!");
        }
        if (null == keysets) {
            throw new IllegalArgumentException("keysets cannot be null!");
        }
        if (null == fuzzers) {
            throw new IllegalArgumentException("fuzzers cannot be null!");
        }

        _writer = writer;
        _TARs = TARs;
        _keysets = keysets;
        _fuzzers = fuzzers;
    }

    public boolean isThereAWeaknessFound() {
        return (signedResponses.size() + encryptedResponses.size() + unprotectedTARsResponses.size() + decryptionOracleResponses.size() + wibCommandExecuted.size() + satCommandExecuted.size()) > 0;
    }

    private void scanAPDUonUnprotectedEntryPoints() {
        ArrayList<EntryPoint> unprotectedEntryPoints = new ArrayList<>();

        for (FuzzerResult fr : unprotectedTARsResponses) {
            // ARD has to be present too to actually scan APDUs otherwise we're just blind
            if (fr._responsePacket.getStatusCode() == 0x00 && fr._responsePacket.areAdditionalDataPresent()) {
                unprotectedEntryPoints.add(new EntryPoint(fr._commandPacket.getTAR(), fr._commandPacket.getKeyset(), fr._commandPacket, fr._responsePacket));
            }
        }

        unprotectedEntryPoints = new ArrayList<>(new HashSet(unprotectedEntryPoints)); // make the results unique
        Collections.sort(unprotectedEntryPoints, new Comparator<EntryPoint>() {
            @Override
            public int compare(EntryPoint ep1, EntryPoint ep2) {
                return HexToolkit.compareTARs(ep1.getTAR(), ep2.getTAR());
            }
        });

        if (unprotectedEntryPoints.size() < 1) {
            return; // no point in doing anything as there are no unprotected TARs that provide ARD
        }

        System.out.println();
        System.out.print("Going to perform APDU scan on following TARs: ");
        for (EntryPoint ep : unprotectedEntryPoints) {
            System.out.print(HexToolkit.toString(ep.getTAR()) + " ");
        }
        System.out.println();

        CSVWriter writer = new CSVWriter(SIMTester.ICCID, "APDU", SIMTester._logging);
        writer.writeBasicInfo(SIMTester.ATR, SIMTester.ICCID, SIMTester.IMSI, SIMTester.MSISDN, SIMTester.EF_MANUAREA, SIMTester.EF_DIR, SIMTester.AUTH, SIMTester.AppDeSelect);

        for (EntryPoint ep : unprotectedEntryPoints) {
            try {
                APDUScanner.run(ep, writer, true, SIMTester.cmdline.hasOption("sal2"));
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }
            } catch (CardException e) {
                e.printStackTrace(System.err);
            }
        }

        if (!writer.unhideFile()) {
            System.err.println(LoggingUtils.formatDebugMessage("Unable to unhide file " + writer.getFileName() + ", make sure you rename it so it does NOT start with a dot to get processed!"));
        } else if (SIMTester._gsmmap_upload) {
            if (GSMMapUploader.uploadFile(writer.getFileName())) {
                System.out.println("Upload of " + writer.getFileName() + " to gsmmap.org successful!");
            } else {
                System.err.println("There was a problem uploading the result to gsmmap.org");
                System.err.println("Please use the form at http://gsmmap.org/upload.html to submit the data manually.");
            }
        }

    }

    @Override
    public void run() {
        try {
            for (String TAR : _TARs) {
                this.logic(TAR, _keysets, _fuzzers);
            }
            if (unprotectedTARsResponses.size() > 0 && !Thread.currentThread().isInterrupted()) {
                scanAPDUonUnprotectedEntryPoints();
            }
        } catch (Exception ex) { // lol what a mess
            Thread t = Thread.currentThread();
            t.getUncaughtExceptionHandler().uncaughtException(t, ex);
        }
    }

    private ResponseAPDU fuzzCard(CommandPacket cp) throws Exception {
        SMSDeliverTPDU smsdeliver = new SMSDeliverTPDU();
        //smsdeliver.setTPOA(); // not yet implemented, there's a hardcoded OA, sufficient for this kind of "fuzzing" as we don't expect responses to be sent out to the real network
        smsdeliver.setTPUDHI(true); // User Data Header Indicator, message contains user data header in additional to the message

        if (DEBUG) {
            System.out.println(LoggingUtils.formatDebugMessage("smsdeliver data: " + HexToolkit.toString(smsdeliver.getBytes())));
        }

        if (null != KIC) {
            cp.setFakeKIC(KIC);
        }

        if (null != KID) {
            cp.setFakeKID(KID);
        }

        if (null != SPI1) {
            cp.setFakeSPI1(SPI1);
        }

        if (null != SPI2) {
            cp.setFakeSPI2(SPI2);
        }

        smsdeliver.setTPUD(cp.getBytes()); // User data

        SMSTPDU smstpdu = new SMSTPDU(smsdeliver.getBytes());

        Address addr;
        if (SIMLibrary.third_gen_apdu) {
            addr = new Address(HexToolkit.fromString("86050021436587"));
        } else {
            addr = new Address(HexToolkit.fromString("06050021436587"));
        }

        EnvelopeSMSPPDownload env = new EnvelopeSMSPPDownload(addr, smstpdu);

        CommandAPDU envelope = env.getAPDU();

        if (DEBUG) {
            System.out.println(LoggingUtils.formatDebugMessage("Envelope content: " + HexToolkit.toString(envelope.getBytes())));
        }

        ResponseAPDU response = ChannelHandler.transmitOnDefaultChannel(envelope);

        return response;
    }

    public static CommandPacket generateCommandPacket(int keyset, byte counterManagement, int KICAlgo, int KIDAlgo, String TAR, boolean requestPoR, boolean cipherPoR) {

        if (DEBUG) {
            System.out.println(LoggingUtils.formatDebugMessage("called generateCommandPacket(keyset = " + keyset + ", counterManagement = " + counterManagement + ", KICAlgo = " + KICAlgo + ", KIDAlgo = " + KIDAlgo + ", TAR = " + TAR + ", requestPoR = " + requestPoR + ", cipherPoR = " + cipherPoR));
        }

        String[] split_TAR = TAR.split(":");

        CommandPacket cp = new CommandPacket();
        cp.setKeyset(keyset); // choose keyset number

        cp.setKIDAlgo(KIDAlgo); // algorithm used for KID parameter (second number of KID param, 1 = DES, 5 = 3DES) !! use constant from CommandPacket class !!
        if (KIDAlgo == CommandPacket.KID_ALGO_DES_CBC) {
            cp.setCryptographicChecksum(true, HexToolkit.fromString("0000000000000000")); // KID key for cryptographic checksum, either change to a real one or just use a fake one to have something to sign with (you'll perhaps get a invalid CC error, MAY LOCK THE KEYSET!)
        } else if (KIDAlgo == CommandPacket.KID_ALGO_3DES_CBC_2KEYS) {
            cp.setCryptographicChecksum(true, HexToolkit.fromString("00000000000000000000000000000000"));
        } else if (KIDAlgo == CommandPacket.KID_ALGO_3DES_CBC_3KEYS) {
            cp.setCryptographicChecksum(true, HexToolkit.fromString("000000000000000000000000000000000000000000000000"));
        }

        cp.setKICAlgo(KICAlgo); // algorithm used for KIC parameter (second number of KID param, 1 = DES, 5 = 3DES) !! use constant from CommandPacket class !!
        if (KICAlgo == CommandPacket.KIC_ALGO_DES_CBC) {
            cp.setCiphering(true, HexToolkit.fromString("0000000000000000"), false);
        } else if (KICAlgo == CommandPacket.KIC_ALGO_3DES_CBC_2KEYS) {
            cp.setCiphering(true, HexToolkit.fromString("00000000000000000000000000000000"), false);
        } else if (KICAlgo == CommandPacket.KIC_ALGO_3DES_CBC_3KEYS) {
            cp.setCiphering(true, HexToolkit.fromString("000000000000000000000000000000000000000000000000"), false);
        }

        cp.setCounterManegement(counterManagement); // counter management verification bit !! use constant from CommandPacket class !!
        if (counterManagement == CommandPacket.CNTR_NO_CNTR_AVAILABLE) {
            cp.setCounter(0);
        } else {
            cp.setCounter(1);
        }

        // static setting for PoR, get PoR, sign it, send it via SMS DELIVER REPORT
        if (requestPoR) {
            cp.setPoR(true); // request PoR

            if (cipherPoR) {
                cp.setPoRCiphering(true);
            } else {
                cp.setPoRSecurity(CommandPacket.POR_SECURITY_CC); // sign the PoR packet
            }

            if (use_sms_submit) {
                cp.setPoRMode(CommandPacket.POR_MODE_SMS_SUBMIT);
            } else {
                cp.setPoRMode(CommandPacket.POR_MODE_SMS_DELIVER_REPORT);
            }
        }

        cp.setTAR(HexToolkit.fromString(split_TAR[1]));

        switch (split_TAR[0]) {
            case "RFM":
                cp.setUserData(HexToolkit.fromString("A0A40000023F00")); // RFM, A0A40000023F00 = 2G selectFile MF (MasterFile)
                break;
            case "RAM":
                cp.setUserData(HexToolkit.fromString("80E60200160BA000000000123456789010000006EF04C602000000")); // RAM, Install for Load, AID = A000000000123456789010
                break;
            case "WIB":
                String wibTestPayload = "0016100102140801912143658709F0200500000001010600"; // WIB, SETUP CALL +12345678900
                cp.setUserData(HexToolkit.fromString(wibTestPayload));
                break;
            case "SAT":
                String satTestPayload = "42230121020744382E3130353105160604313035312D0C1003830607912143658709F02B00"; // S@T, SETUP CALL +12345678900
                cp.setUserData(HexToolkit.fromString(satTestPayload));
                break;
            default:
                System.err.println(LoggingUtils.formatDebugMessage("Unsupported TAR type: " + split_TAR[0] + ", exiting.."));
                System.exit(1);
                break;
        }

        return cp;
    }

    public void logic(String TAR, List keysets, ArrayList<FuzzerData> fuzzers) throws Exception {

        // we're going to try all the fuzzing steps on all the keysets
        for (Iterator<FuzzerData> fuzzIter = fuzzers.iterator(); fuzzIter.hasNext() && !Thread.currentThread().isInterrupted();) {

            ListIterator<Integer> keyset_iter = keysets.listIterator();
            FuzzerData fuzzer = fuzzIter.next();
            String fuzzerName = fuzzer._name;

            while (keyset_iter.hasNext() && !Thread.currentThread().isInterrupted()) {

                Integer keyset = (Integer) keyset_iter.next();

                CommandPacket cp = generateCommandPacket(keyset, fuzzer._counter, fuzzer._kic, fuzzer._kid, TAR, fuzzer._requestPoR, fuzzer._cipherPoR);

                ResponseAPDU response;
                try {
                    response = fuzzCard(cp);
                } catch (CardException e) {
                    System.out.println(LoggingUtils.formatDebugMessage("Card probably crashed, skipping keyset.."));
                    continue;
                }

                if ((byte) response.getSW1() == (byte) 0x90 && (byte) response.getSW2() == (byte) 0x00) {
                    System.out.println("\033[90m" + "fuzzer: " + fuzzerName + ", TAR: " + HexToolkit.toString(cp.getTAR()) + ", keyset: " + keyset + " - no PoR packet even if requested (SW: 0x9000)" + "\033[0m");
                    _writer.writeLine(fuzzerName, cp.getBytes(), response.getBytes());
                    continue;
                }

                byte[] response_data = null;

                response_data = getOTAResponse(response,cp,_writer,fuzzer,TAR,keyset);

                if (null == response_data) {
                    System.out.println("Unable to locate Response Packet Header in fetched data, skipping..");
                    continue;
                }

                if (response_data[0] == (byte) 0xd0) {
                    ResponsePacket rp = new ResponsePacket();
                    rp.parse(response_data, false);
                    if (TAR.equals("SAT:505348")) {
                        satCommandExecuted.add(new FuzzerResult(cp, fuzzer, rp));
                    } else {
                        wibCommandExecuted.add(new FuzzerResult(cp, fuzzer, rp));                            
                    }
                    System.out.println("\033[91mfuzzer: " + fuzzerName + ", TAR: " + HexToolkit.toString(cp.getTAR())
                    + ", keyset: " + cp.getKeyset() + ", Command executed, response: " + HexToolkit.toString(response_data) + " -> CRITICAL WEAKNESS FOUND\033[0m");

                    continue;
                }

                if (null != response_data) {
                    if (!handleResponseData(cp, response_data, fuzzer)) {
                        return; // we want to skip this TAR completely
                    }
                    continue;
                }

                System.out.println("\033[90m" + "fuzzer: " + fuzzerName + ", TAR: " + HexToolkit.toString(cp.getTAR()) + ", keyset: " + keyset + " - unexpected card response (" + HexToolkit.toString(response.getBytes()) + "), check it out!" + "\033[0m");
                _writer.writeLine(fuzzerName, cp.getBytes(), response.getBytes());
            }
        }
        // if we've already scanned a TAR that returned PoR 09 let's skip all others
        // as they are not going to show any different signatures
        // Unless the card leaks even on PoR 0x09 - then we want to go over everything.
        if (scannedTARwithPoR09) {
            boolean PoR09leaks = false;

            for (FuzzerResult encrypted : encryptedResponses) {
                // there is encrypted response with PoR 0x09 (card leaks even on PoR 09)
                if (encrypted._responsePacket.getStatusCode() == (byte) 0x09 && encrypted._responsePacket.isEncrypted() && encrypted._responsePacket.areAdditionalDataPresent()) {
                    PoR09leaks = true;
                }
            }

            for (FuzzerResult signed : signedResponses) {
                // there is signed response with PoR 0x09 (card leaks even on PoR 09)
                if (signed._responsePacket.getStatusCode() == (byte) 0x09 && !signed._responsePacket.isEncrypted() && signed._responsePacket.isCryptographicChecksumPresent()) {
                    PoR09leaks = true;
                }
            }

            if (!PoR09leaks) {
                skipTAROnPoR09 = true;
            } else {
                System.out.println("Process will not skip PoR 0x09 as card leaks even on PoR 0x09, let's gather all that.");
            }
        }
    }

    public boolean handleResponseData(CommandPacket cp, byte[] response_data, FuzzerData fuzzer) {
        ResponsePacket rp = new ResponsePacket();

        try {
            rp.parse(response_data, cp.getPoRCounter());
        } catch (ParseException e) {
            System.err.println("Parse exception while parsing response packet, skipping it! details:");
            e.printStackTrace(System.err);
            return true;
        }

        boolean warning = false;
        boolean critical = false;
        byte status_code = rp.getStatusCode();

        if (rp.isEncrypted()) { // if the RP is encrypted it's pointless to test for PoR
            warning = true;
            encryptedResponses.add(new FuzzerResult(cp, fuzzer, rp));
        } else if (status_code == (byte) 0x00 || status_code == (byte) 0x02 || status_code == (byte) 0x03) { // PoR OK or CNTR low or CNTR high
            critical = true;
            unprotectedTARsResponses.add(new FuzzerResult(cp, fuzzer, rp));
        }

        String PoRCC;
        if (rp.isCryptographicChecksumPresent() && !rp.isEncrypted()) { // if the RP is encrypted it's pointless to check for the CC
            if (!"0000000000000000".equals(HexToolkit.toString(rp.getCryptographicChecksum()))) {
                warning = true;
                signedResponses.add(new FuzzerResult(cp, fuzzer, rp));
            }
            PoRCC = HexToolkit.toString(rp.getCryptographicChecksum());
        } else {
            PoRCC = null;
        }

        String fuzzerName = fuzzer._name;

        if (status_code == (byte) 0x09 && !rp.isEncrypted()) { // if the RP is encrypted it's pointless to test for PoR
            scannedTARwithPoR09 = true;
            if (skipTAROnPoR09) {
                System.out.println("\033[90m" + "fuzzer: " + fuzzerName + ", TAR: " + HexToolkit.toString(cp.getTAR()) + " -> unknown to card (PoR 0x09), already scanned, skipping.." + "\033[0m");
                return false;
            }
        }

        if (rp.isDecryptedCounter()) {//look for decryption oracle (decrypting counter from command)
            warning = true;
            decryptionOracleResponses.add(new FuzzerResult(cp, fuzzer, rp));
        }

        System.out.print((critical ? "\033[91m" : (warning ? "\033[93m" : "\033[90m")));

        if (rp.isEncrypted()) { // PoR is requested in encrypted form
            System.out.print("fuzzer: " + fuzzerName + ", TAR: " + HexToolkit.toString(cp.getTAR())
                    + ", keyset: " + cp.getKeyset() + ", ResponsePacket: " + HexToolkit.toString(response_data));

        } else {
            System.out.print("fuzzer: " + fuzzerName + ", TAR: " + HexToolkit.toString(cp.getTAR())
                    + ", keyset: " + cp.getKeyset() + ", PoR: " + HexToolkit.toString(status_code) + ", PoR CC: " + PoRCC);
            if (rp.areAdditionalDataPresent()) {
                System.out.print(", ARD: " + HexToolkit.toString(rp.getAdditionalData()));
            }
        }


        System.out.println(((critical | warning) ? (critical ? " -> CRITICAL WEAKNESS FOUND\033[0m" : " -> WEAKNESS FOUND\033[0m") : "\033[0m"));

        return true;
    }

    public static ResponseAPDU applicationDeSelect() throws Exception {
        // Lc = 00h, that's why there's a byte array, as we do not want to pass any data and CommandAPDU constructs calculate Lc automatically or don't trasmit it
        CommandAPDU sel = new CommandAPDU(new byte[]{(byte) 0x00, (byte) 0xA4, (byte) 0x04, (byte) 0x00, (byte) 0x00}); // SelectApplication with no name (de-select, selects default application and return its AID in additional data
        return ChannelHandler.transmitOnDefaultChannel(sel, false); // do not retry this, if it fails just skip it
    }

    public static byte[] getOTAResponse(ResponseAPDU response, CommandPacket cp, CSVWriter writer, FuzzerData fuzzer, String TAR, Integer keyset) throws CardException{
        byte[] response_data = null;
        String fuzzerName = fuzzer._name;

        if ((byte) response.getSW1() == (byte) 0x9E || (byte) response.getSW1() == (byte) 0x9F
                || (SIMLibrary.third_gen_apdu && ((byte) response.getSW1() == (byte) 0x62 || (byte) response.getSW1() == (byte) 0x61))) {
            ResponseAPDU getresponse_response = APDUToolkit.getResponse(response.getSW2());
            response_data = getresponse_response.getData();
            writer.writeLine(fuzzerName, cp.getBytes(), getresponse_response.getBytes()); // log full response
        } else if ((byte) response.getSW1() == (byte) 0x91) { // fetch
            ResponseAPDU fetch_response = APDUToolkit.performFetch(response.getSW2());
            byte[] fetched_data = fetch_response.getData();
            System.out.println("\033[90m" + "fuzzer: " + fuzzerName + ", TAR: " + TAR + ", keyset: " + keyset + " - card responded with FETCH, fetched_data = " + HexToolkit.toString(fetched_data) + ", response word: " + String.format("%04X", fetch_response.getSW()) + "\033[0m");
            writer.writeLine(fuzzerName, cp.getBytes(), fetch_response.getBytes()); // log full fetch response

            if (fetched_data[0] == (byte) 0xD0) { // handling of proactive data..
                ProactiveCommand pc;
                try {
                    pc = new ProactiveCommand(fetched_data);

                    String summary = pc.getSummary();
                    if (!"".equals(summary)) {
                        System.out.println("\033[90m" + "Proactive command (" + "\033[95m" + pc.getType() + "\033[90m" + ") identified, details: " + "\033[95m" + summary + "\033[90m" + "; trying to handle it.." + "\033[0m");
                    } else {
                        System.out.println("\033[90m" + "Proactive command (" + "\033[95m" + pc.getType() + "\033[90m" + ") identified, trying to handle it.." + "\033[0m");
                    }

                    if (pc.getType().equals("SETUP CALL")) {
                        ResponseAPDU handled_response = AutoTerminalProfile.handleProactiveCommand(fetch_response);
                        return fetched_data;
                    }                    
                } catch (ParseException e) {
                    System.err.println(LoggingUtils.formatDebugMessage("Unable to parse ProactiveCommand, skipping its handling, this may get ugly !!!"));
                }

                ResponseAPDU handled_response = AutoTerminalProfile.handleProactiveCommand(fetch_response);

                int limit = 0;
                while (handled_response.getSW() != 0x9000 && limit < 10) {
                    System.out.println("\033[95m" + "WARNING! Response (SW) to terminal response apdu is not 0x9000: " + HexToolkit.toString(handled_response.getBytes()) + "\033[0m");
                    handled_response = Helpers.handleSIMResponse(handled_response, true);
                    limit++;
                }

                if (limit == 10) {
                    System.err.println("FATAL ERROR: endless loop while handling response! SCAN IS INCOMPLETE! FIX THIS!");
                    System.exit(1);
                }

            }

            response_data = ResponsePacket.Helpers.findResponsePacket(fetched_data);
        }
        return response_data;
    }
}
