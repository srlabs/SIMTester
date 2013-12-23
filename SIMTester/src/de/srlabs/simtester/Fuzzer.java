package de.srlabs.simtester;

import de.srlabs.simlib.APDUToolkit;
import de.srlabs.simlib.Address;
import de.srlabs.simlib.AutoTerminalProfile;
import de.srlabs.simlib.ChannelHandler;
import de.srlabs.simlib.CommandPacket;
import de.srlabs.simlib.EnvelopeSMSPPDownload;
import de.srlabs.simlib.HexToolkit;
import de.srlabs.simlib.LoggingUtils;
import de.srlabs.simlib.ResponsePacket;
import de.srlabs.simlib.SMSDeliverTPDU;
import de.srlabs.simlib.SMSTPDU;
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
    private final static boolean DEBUG = Main.DEBUG || LOCAL_DEBUG;
    private static boolean scannedTARwithPoR09 = false;
    private static boolean skipTAROnPoR09 = false;
    public static Byte KIC = null;
    public static Byte KID = null;
    public static Byte SPI1 = null;
    public static Byte SPI2 = null;
    private CSVWriter _writer;
    private List<String> _TARs = null;
    private List _keysets = null;
    private ArrayList<FuzzerData> _fuzzers = null;
    public ArrayList<FuzzerResult> signedResponses = new ArrayList<FuzzerResult>();
    public ArrayList<FuzzerResult> encryptedResponses = new ArrayList<FuzzerResult>();
    public ArrayList<FuzzerResult> unprotectedTARsResponses = new ArrayList<FuzzerResult>();

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
        return (signedResponses.size() + encryptedResponses.size() + unprotectedTARsResponses.size()) > 0;
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
        
        CSVWriter writer = new CSVWriter(Main.ICCID, "APDU", Main._logging);
        writer.writeBasicInfo(Main.ATR, Main.ICCID, Main.IMSI, Main.EF_MANUAREA, Main.EF_DIR, Main.AppDeSelect);

        for (EntryPoint ep : unprotectedEntryPoints) {
            try {
                APDUScanner.run(ep, writer, true, false);
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }
            } catch (CardException e) {
                e.printStackTrace(System.err);
            }
        }
        
        writer.unhideFile();
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

        smsdeliver.setTPUD(cp.getBytes()); // User data

        SMSTPDU smstpdu = new SMSTPDU(smsdeliver.getBytes());

        Address addr = new Address(HexToolkit.fromString("06050021436587"));
        EnvelopeSMSPPDownload env = new EnvelopeSMSPPDownload(addr, smstpdu);

        CommandAPDU envelope = env.getAPDU();
        ResponseAPDU response = ChannelHandler.getDefaultChannel().transmit(envelope);

        return response;
    }

    private CommandPacket generateCommandPacket(int keyset, byte counterManagement, int KICAlgo, int KIDAlgo, String TAR, boolean cipherPoR) {

        if (DEBUG) {
            System.out.println(LoggingUtils.formatDebugMessage("called generateCommandPacket(keyset = " + keyset + ", counterManagement = " + counterManagement + ", KICAlgo = " + KICAlgo + ", KIDAlgo = " + KIDAlgo + ", TAR = " + TAR + ", cipherPoR = " + cipherPoR));
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
            cp.setCiphering(true, HexToolkit.fromString("0000000000000000"));
        } else if (KICAlgo == CommandPacket.KIC_ALGO_3DES_CBC_2KEYS) {
            cp.setCiphering(true, HexToolkit.fromString("00000000000000000000000000000000"));
        } else if (KICAlgo == CommandPacket.KIC_ALGO_3DES_CBC_3KEYS) {
            cp.setCiphering(true, HexToolkit.fromString("000000000000000000000000000000000000000000000000"));
        }

        cp.setCounterManegement(counterManagement); // counter management verification bit !! use constant from CommandPacket class !!
        cp.setCounter(1);

        // static setting for PoR, get PoR, sign it, send it via SMS DELIVER REPORT
        cp.setPoR(true); // request PoR
        if (cipherPoR) {
            cp.setPoRCiphering(true);
        } else {
            cp.setPoRSecurity(CommandPacket.POR_SECURITY_CC); // sign the PoR packet
        }
        cp.setPoRMode(CommandPacket.POR_MODE_SMS_DELIVER_REPORT);
        //cp.setPoRMode(CommandPacket.POR_MODE_SMS_SUBMIT);

        cp.setTAR(HexToolkit.fromString(split_TAR[1]));

        if ("RFM".equals(split_TAR[0])) {
            cp.setUserData(HexToolkit.fromString("A0A40000023F00")); // RFM, A0A40000023F00 = 2G selectFile MF (MasterFile)
        } else if ("RAM".equals(split_TAR[0])) {
            cp.setUserData(HexToolkit.fromString("80E60200160BA000000000123456789010000006EF04C602000000")); // RAM, Install for Load, AID = A000000000123456789010
        } else {
            System.err.println(LoggingUtils.formatDebugMessage("Unsupported TAR type: " + split_TAR[0] + ", exiting.."));
            System.exit(1);
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

                CommandPacket cp = generateCommandPacket(keyset, fuzzer._counter, fuzzer._kic, fuzzer._kid, TAR, fuzzer._cipherPoR);
                ResponseAPDU response = fuzzCard(cp);

                if ((byte) response.getSW1() == (byte) 0x90 && (byte) response.getSW2() == (byte) 0x00) {
                    System.out.println("fuzzer: " + fuzzerName + ", TAR: " + HexToolkit.toString(cp.getTAR()) + ", keyset: " + keyset + " - no PoR packet even if requested (SW: 0x9000)");
                    _writer.writeLine(fuzzerName, cp.getBytes(), response.getBytes());
                    continue;
                }

                byte[] response_data = null;

                if ((byte) response.getSW1() == (byte) 0x9E || (byte) response.getSW1() == (byte) 0x9F) {
                    response_data = APDUToolkit.getResponse(response.getSW2()).getData();
                } else if ((byte) response.getSW1() == (byte) 0x91) { // fetch
                    ResponseAPDU fetch_response = APDUToolkit.performFetch(response.getSW2());
                    byte[] fetched_data = fetch_response.getData();
                    System.out.println("fuzzer: " + fuzzerName + ", TAR: " + TAR + ", keyset: " + keyset + " - card responded with FETCH, fetched_data = " + HexToolkit.toString(fetched_data));

                    if (fetched_data[0] == (byte) 0xD0) { // handling of proactive data..
                        System.out.println("first byte of fetch data is 0xD0, trying to handle the proactive command we fetched.. ");
                        AutoTerminalProfile.handleProactiveCommand(fetch_response);
                    }

                    response_data = ResponsePacket.Helpers.findResponsePacket(fetched_data);
                    if (null == response_data) {
                        System.out.println("Unable to locate Response Packet Header in fetched data, skipping..");
                        continue;
                    }
                }

                if (null != response_data) {
                    if (!handleResponseData(cp, response_data, fuzzer)) {
                        return; // we want to skip this TAR completely
                    }
                    continue;
                }

                System.out.println("fuzzer: " + fuzzerName + ", TAR: " + HexToolkit.toString(cp.getTAR()) + ", keyset: " + keyset + " - unexpected card response, check it out!");
                System.out.println(HexToolkit.toString(response.getBytes()));
                _writer.writeLine(fuzzerName, cp.getBytes(), response.getBytes());
            }
        }
        // if we've already scanned a TAR that returned PoR 09 let's skip all others
        // as they are not going to show any different signatures
        if (scannedTARwithPoR09) {
            skipTAROnPoR09 = true;
        }
    }

    private boolean handleResponseData(CommandPacket cp, byte[] response_data, FuzzerData fuzzer) {
        ResponsePacket rp = new ResponsePacket();
        rp.parse(response_data, cp.getPoRCounter());

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
        _writer.writeLine(fuzzerName, cp.getBytes(), response_data);

        if (status_code == (byte) 0x09 && !rp.isEncrypted()) { // if the RP is encrypted it's pointless to test for PoR
            scannedTARwithPoR09 = true;
            if (skipTAROnPoR09) {
                System.out.println("fuzzer: " + fuzzerName + ", TAR: " + HexToolkit.toString(cp.getTAR()) + " -> unknown to card (PoR 0x09), already scanned, skipping..");
                return false;
            }
        }

        System.out.print((critical ? "\033[91m" : (warning ? "\033[93m" : "")));

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

        System.out.println(((critical | warning) ? (critical ? " -> CRITICAL WEAKNESS FOUND\033[0m" : " -> WEAKNESS FOUND\033[0m") : ""));

        return true;
    }

    public static ResponseAPDU applicationDeSelect() throws Exception {
        // Lc = 00h, that's why there's a byte array, as we do not want to pass any data and CommandAPDU constructs calculate Lc automatically or don't trasmit it
        CommandAPDU sel = new CommandAPDU(new byte[]{(byte) 0x00, (byte) 0xA4, (byte) 0x04, (byte) 0x00, (byte) 0x00}); // SelectApplication with no name (de-select, selects default application and return its AID in additional data
        return ChannelHandler.getDefaultChannel().transmit(sel);
    }
}
