package de.srlabs.simtester;

import de.srlabs.simlib.*;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import javax.smartcardio.*;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class SIMTester {

    public static boolean DEBUG = false;
    public static CommandLine cmdline;
    private static String action = "";
    private static List<String> TARs = FuzzerFactory.defaultTARs;
    private static List<Integer> customFuzzers = null;
    private static List<Integer> customKeysets = null;
    private static List<String> customTARs = null;
    private static String _fuzzingLevel = "FULL";
    public static boolean _gsmmap_upload = false;
    private static boolean _skipPin = false;
    public static boolean _logging = true;
    public static String ATR = null;
    public static String ICCID = null;
    public static String IMSI = null;
    public static String MSISDN = null;
    public static String EF_MANUAREA = null;
    public static String EF_DIR = null;
    public static String AUTH = null;
    public static String AppDeSelect = null;
    private static final String version = "SIMTester v2.0.0, 2022-10-25";
    private static Fuzzer _fuzzer = null;
    private static TARScanner _tarscanner = null;
    private static CSVWriter _writer = null;

    public static void main(String[] args) throws Exception {

        System.out.println();
        System.out.println("########################################");
        System.out.println("  " + version);
        System.out.println("  Lukas Kuzmiak    (lukas@srlabs.de)    ");
        System.out.println("  Luca Melette     (luca@srlabs.de)     ");
        System.out.println("  Jonas Schmid     (jonas@srlabs.de)    ");
        System.out.println("  Gabriel Arnautu  (gabriel@srlabs.de)  ");
        System.out.println("  Security Research Labs, Berlin, " + Calendar.getInstance().get(Calendar.YEAR));
        System.out.println("########################################");
        System.out.println();

        Instant startPeriod = Instant.now();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {

                System.out.println();
                System.out.println("Graceful shutdown initiated. Trying to close all open channels. Please wait... !");

                if (null != _fuzzer) {
                    _fuzzer.interrupt();
                    try {
                        _fuzzer.join(); // wait for all actions in thread to finish
                    } catch (InterruptedException ignored) {
                    }
                }

                if (null != _tarscanner) {
                    _tarscanner.interrupt();
                    try {
                        _tarscanner.join(); // wait for all actions in thread to finish
                        _tarscanner.scanExit();
                    } catch (InterruptedException ignored) {
                    }
                }

                try {
                    ChannelHandler.disconnectCard(); // FIXME: this get hung up if layer1 connection does not answer anymore and you have to kill -9 <java>
                } catch (CardException ignored) {
                }

                if (null != _fuzzer) {
                    printSummary(_fuzzer);
                }
                if (null != _writer) {
                    if (!_writer.unhideFile()) {
                        System.err.println(LoggingUtils.formatDebugMessage("Unable to unhide file " + _writer.getFileName() + ", make sure you rename it so it does NOT start with a dot to get processed!"));
                    } else if (_gsmmap_upload) {
                        if (GSMMapUploader.uploadFile(_writer.getFileName())) {
                            System.out.println("Upload of " + _writer.getFileName() + " to gsmmap.org successful!");
                        } else {
                            System.err.println("There was a problem uploading the result to gsmmap.org");
                            System.err.println("Please use the form at http://gsmmap.org/upload.html to submit the data manually.");
                        }
                    }
                }

                System.out.println("Execution time (minutes): " + Duration.between(startPeriod, Instant.now()).toMinutes());
            }
        });

        handleOptions(args);

        switch (action) {
            case "TAR":
                performTARScanning();
                break;
            case "APDU":
                performAPDUScannning();
                break;
            case "OTA":
                performOTAFuzzing();
                break;
            default:
                performStandardFuzzing();
        }
    }

    public static void performStandardFuzzing() throws Exception {
        readBasicInfo();
        _writer = new CSVWriter(ICCID, "FUZZ", _logging);
        _writer.writeBasicInfo(ATR, ICCID, IMSI, MSISDN, EF_MANUAREA, EF_DIR, AUTH, AppDeSelect);
        fuzz();
        _writer.unhideFile();
    }

    public static void performOTAFuzzing() throws Exception {
        readBasicInfo();
        _writer = new CSVWriter(ICCID, "OTA", _logging);
        _writer.writeBasicInfo(ATR, ICCID, IMSI, MSISDN, EF_MANUAREA, EF_DIR, AUTH, AppDeSelect);

        int ota_keyset = 1;
        if (null != customKeysets) {
            ota_keyset = customKeysets.get(0);
        }

        FuzzerData fuzzer = FuzzerFactory.getFuzzer(1);
        if (null != customFuzzers) {
            fuzzer = FuzzerFactory.getFuzzer(customFuzzers.get(0));
        }

        String TAR = "RAM:000000";
        if (null != customTARs) {
            TAR = customTARs.get(0);
        }

        OTAFuzzer.fuzzOTA(ota_keyset, TAR, fuzzer, _writer, cmdline.hasOption("ofbf"));
        _writer.unhideFile();
        System.out.println("done fuzzing OTA passthrough, exiting..");
    }

    public static void performAPDUScannning() throws Exception {
        readBasicInfo();
        _writer = new CSVWriter(ICCID, "APDU", _logging);
        _writer.writeBasicInfo(ATR, ICCID, IMSI, MSISDN, EF_MANUAREA, EF_DIR, AUTH, AppDeSelect);
        APDUScanner.run(null, _writer, false, cmdline.hasOption("sal2"));
        _writer.unhideFile();
        System.out.println("done scanning APDUs, exiting..");
    }

    public static void performTARScanning() throws Exception {
        readBasicInfo();
        _writer = new CSVWriter(ICCID, "TAR", _logging);
        _writer.writeBasicInfo(ATR, ICCID, IMSI, MSISDN, EF_MANUAREA, EF_DIR, AUTH, AppDeSelect);

        int keyset = 1;
        if (cmdline.hasOption("k") && !customKeysets.isEmpty()) {
            keyset = customKeysets.get(0);
        }

        boolean try_being_smart = false;
        if (cmdline.hasOption("stbs")) {
            try_being_smart = true;
        }

        String regexp_to_match_response = null;
        if (cmdline.hasOption("stre")) {
            regexp_to_match_response = cmdline.getOptionValue("stre");
        }

        if (cmdline.hasOption("str")) {
            _tarscanner = new TARScanner("scanRangesOfTARs", keyset, _writer, try_being_smart, regexp_to_match_response);

            if (cmdline.hasOption("t") && !customTARs.isEmpty()) {
                String[] split_TAR = customTARs.get(0).split(":");
                String startingTAR = split_TAR[1];
                _tarscanner.setStartingTAR(startingTAR);
            }

            _tarscanner.start();
            _tarscanner.join();
        } else {
            _tarscanner = new TARScanner("scanAllTARs", keyset, _writer, try_being_smart, regexp_to_match_response);

            if (cmdline.hasOption("t") && !customTARs.isEmpty()) {
                String[] split_TAR = customTARs.get(0).split(":");
                String startingTAR = split_TAR[1];
                _tarscanner.setStartingTAR(startingTAR);
            }

            _tarscanner.start();
            _tarscanner.join();
        }

        _writer.unhideFile();
        System.out.println("done scanning TARs, exiting..");
    }

    private static void readBasicInfo() throws Exception {
        ResponseAPDU res;
        byte[] baATR = ChannelHandler.getDefaultChannel().getCard().getATR().getBytes();
        ATR = HexToolkit.toString(baATR);
        System.out.println();
        System.out.println("ATR: " + ATR);

        if (AutoTerminalProfile.autoTerminalProfile()) {
            if (DEBUG) {
                System.out.println(LoggingUtils.formatDebugMessage("Automatic Terminal profile initialization SUCCESSFUL!"));
            }
        } else {
            if (DEBUG) {
                System.out.println(LoggingUtils.formatDebugMessage("Automatic Terminal profile initialization FAILED!"));
            }
        }

        ICCID = CommonFileReader.readICCID();
        EF_MANUAREA = CommonFileReader.readMANUAREA();
        System.out.println("ICCID: " + ICCID);

        // Set EF_DIR
        ArrayList<byte[]> dirRecords = CommonFileReader.readDIR();
        if (dirRecords.size() > 0) {
            EF_DIR = dirRecords.stream().map(HexToolkit::toString).collect(Collectors.joining(";"));

            // Print EF_DIR content
            System.out.format("The EF_DIR has %d record(s)\n", dirRecords.size());
            for (int i = 0; i < dirRecords.size(); i++) {
                System.out.format("Record %d: %s\n", i, HexToolkit.toString(dirRecords.get(i)));
            }
        }

        byte[] rawIMSI = CommonFileReader.readRawIMSI();
        if (null != rawIMSI) {
            IMSI = CommonFileReader.swapIMSI(rawIMSI);
        } else {
            IMSI = null;
            if (!_skipPin) {
                System.err.println(LoggingUtils.formatDebugMessage("IMSI couldn't be read (verify the pin?)"));
                System.exit(1);
            }
        }

        System.out.println("IMSI: " + IMSI);

        byte[] msisdn = CommonFileReader.readRawMSISDN();
        if (null != msisdn) {
            MSISDN = CommonFileReader.decodeMSISDN(msisdn);
        } else {
            MSISDN = null;
        }

        System.out.println("MSISDN: " + MSISDN);
        System.out.println("EF_MANUAREA: " + EF_MANUAREA);
        System.out.println("EF_DIR: " + EF_DIR);

        if (SIMLibrary.third_gen_apdu) {
            String usimAID = CommonFileReader.getUSIMAID();
            if (usimAID == null) {
                throw new CardException("There is no USIM available.");
            }

            FileManagement.selectAID(HexToolkit.fromString(usimAID));

            byte[] challenge = new byte[17];
            Arrays.fill(challenge, (byte) 0x00);
            challenge[0] = (byte) 16;
            res = APDUToolkit.authenticate(true, challenge); // we HAVE TO use Authenticate APDU in GSM context as we can't provide a valid MAC

            if ((byte) res.getSW1() == (byte) 0x61) {
                res = APDUToolkit.getResponse(res.getSW2());
                AUTH = "3G_" + HexToolkit.toString(res.getData());
            } else if ((short) res.getSW() == (short) 0x9000) {
                AUTH = "3G_" + HexToolkit.toString(res.getBytes());
            } else {
                System.err.println("\033[96m" + "3G AUTH FAILED, " + String.format("%04X", res.getSW()) + " returned. Please investigate, fix and try again...\033[0m");
                System.exit(1);
            }

            ChannelHandler.getInstance().reset();
        } else {
            byte[] rand = new byte[16];
            Arrays.fill(rand, (byte) 0x00);
            res = APDUToolkit.runGSMAlgo2G(rand);
            if ((byte) res.getSW1() == (byte) 0x9F) {
                res = APDUToolkit.getResponse(res.getSW2());
                AUTH = "2G_" + HexToolkit.toString(res.getData());
            } else {
                System.err.println("\033[96m" + "2G AUTH FAILED, " + String.format("%04X", res.getSW()) + " returned. Please investigate, fix and try again...\033[0m");
                System.exit(1);
            }
        }

        System.out.println("AUTH: " + AUTH);

        ResponseAPDU deselectResponse = Fuzzer.applicationDeSelect();
        if (null != deselectResponse) {
            AppDeSelect = HexToolkit.toString(deselectResponse.getBytes());
        } else {
            AppDeSelect = null;
        }
        System.out.println("AppDeSelect: " + AppDeSelect);

        ChannelHandler.getInstance().reset();

        if (AutoTerminalProfile.autoTerminalProfile()) {
            if (DEBUG) {
                System.out.println(LoggingUtils.formatDebugMessage("Automatic Terminal profile initialization SUCCESSFUL!"));
            }
        } else {
            if (DEBUG) {
                System.out.println(LoggingUtils.formatDebugMessage("Automatic Terminal profile initialization FAILED!"));
            }
        }
    }

    private static void fuzz() throws Exception {

        System.out.println();
        System.out.println("Starting fuzzing!");
        System.out.println("Fuzzing level: " + _fuzzingLevel);
        System.out.println();

        ArrayList<FuzzerData> fuzzers = new ArrayList<>();
        List<Integer> keysets = null;
        switch (_fuzzingLevel) {
            case "FULL":
                fuzzers.addAll(FuzzerFactory.getAllFuzzers());
                keysets = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15));
                break;
            case "QUICK":
                fuzzers.add(FuzzerFactory.getFuzzer(1));
                fuzzers.add(FuzzerFactory.getFuzzer(5));
                fuzzers.add(FuzzerFactory.getFuzzer(9));
                fuzzers.add(FuzzerFactory.getFuzzer(13));
                keysets = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5, 6));
                break;
            case "POKE":
                fuzzers.add(FuzzerFactory.getFuzzer(1));
                fuzzers.add(FuzzerFactory.getFuzzer(5));
                fuzzers.add(FuzzerFactory.getFuzzer(9));
                fuzzers.add(FuzzerFactory.getFuzzer(13));
                keysets = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5, 6));
                TARs = new ArrayList<>();
                TARs.add("RAM:000000");
                TARs.add("RFM:B00001");
                TARs.add("RFM:B00010");
                break;
        }

        if (null != customFuzzers) {
            fuzzers.clear();
            for (Integer oneFuzzer : customFuzzers) {
                if (oneFuzzer < 0 || oneFuzzer > FuzzerFactory.getAmountOfFuzzers()) {
                    System.err.println(LoggingUtils.formatDebugMessage("Fuzzer(s) you specified does NOT exist, use values 0-" + FuzzerFactory.getAmountOfFuzzers() + "!"));
                    System.exit(1);
                }
                fuzzers.add(FuzzerFactory.getFuzzer(oneFuzzer));
            }
        }

        if (null != customKeysets) {
            keysets = new ArrayList<>(customKeysets);
        }

        if (null != customTARs) {
            TARs = new ArrayList<>();
            TARs.addAll(customTARs);
        }

        System.out.println("TAR values to be fuzzed: " + TARs);
        System.out.println();

        _fuzzer = new Fuzzer(_writer, TARs, keysets, fuzzers);
        _fuzzer.start();
        _fuzzer.join();
    }

    private static void printSummary(Fuzzer fuzzer) {
        System.out.println();
        if (fuzzer.isThereAWeaknessFound()) {
            if (fuzzer.unprotectedTARsResponses.size() > 0 || fuzzer.wibCommandExecuted.size() > 0 || fuzzer.satCommandExecuted.size() > 0 ) {
                System.out.println("\033[91mSIMTester has discovered following weaknesses:\033[0m");
            } else {
                System.out.println("\033[93mSIMTester has discovered following weaknesses:\033[0m");
            }
            if (fuzzer.signedResponses.size() > 0) {
                System.out.println();
                System.out.println("The following TARs/keysets returned a signed response that may be crackable:");
                System.out.printf("%-6s %6s %s", "TAR", "keyset", "Cryptographic checksums");
                fuzzer.signedResponses = new ArrayList<>(new HashSet(fuzzer.signedResponses)); // make the results unique
                Collections.sort(fuzzer.signedResponses, new FuzzerResultComparator()); // sort them by TAR
                FuzzerResult previous_fr = null;
                for (FuzzerResult fr : fuzzer.signedResponses) {
                    if (null != previous_fr && Arrays.equals(previous_fr._commandPacket.getTAR(), fr._commandPacket.getTAR()) && previous_fr._commandPacket.getKeyset() == fr._commandPacket.getKeyset()) {
                        System.out.printf(" %s", HexToolkit.toString(fr._responsePacket.getCryptographicChecksum()));
                    } else {
                        System.out.printf("\n%-6s %6s %s", HexToolkit.toString(fr._commandPacket.getTAR()), fr._commandPacket.getKeyset(), HexToolkit.toString(fr._responsePacket.getCryptographicChecksum()));
                    }
                    previous_fr = fr;
                }
                System.out.println();
            }
            if (fuzzer.encryptedResponses.size() > 0) {
                System.out.println();
                System.out.println("The following TARs/keysets returned an encrypted response that may be crackable:");
                System.out.printf("%-6s %6s %s\n", "TAR", "keyset", "Response packet");
                fuzzer.encryptedResponses = new ArrayList<>(new HashSet(fuzzer.encryptedResponses)); // make the results unique
                Collections.sort(fuzzer.encryptedResponses, new FuzzerResultComparator()); // sort them by TAR
                FuzzerResult previous_fr = null;
                for (FuzzerResult fr : fuzzer.encryptedResponses) {
                    if (null != previous_fr && Arrays.equals(previous_fr._commandPacket.getTAR(), fr._commandPacket.getTAR()) && previous_fr._commandPacket.getKeyset() == fr._commandPacket.getKeyset()) {
                        System.out.printf(" %s", HexToolkit.toString(fr._responsePacket.getBytes()));
                    } else {
                        System.out.printf("\n%-6s %6s %s", HexToolkit.toString(fr._commandPacket.getTAR()), fr._commandPacket.getKeyset(), HexToolkit.toString(fr._responsePacket.getBytes()));
                    }
                    previous_fr = fr;
                }
                System.out.println();
            }
            if (fuzzer.unprotectedTARsResponses.size() > 0) {
                System.out.println();
                System.out.println("The following TARs/keysets returned a valid response without any security:");
                System.out.printf("%-6s %6s %s\n", "TAR", "keyset", "Response packets");
                fuzzer.unprotectedTARsResponses = new ArrayList<>(new HashSet(fuzzer.unprotectedTARsResponses)); // make the results unique
                Collections.sort(fuzzer.unprotectedTARsResponses, new FuzzerResultComparator()); // sort them by TAR
                FuzzerResult previous_fr = null;
                for (FuzzerResult fr : fuzzer.unprotectedTARsResponses) {
                    if (null != previous_fr && Arrays.equals(previous_fr._commandPacket.getTAR(), fr._commandPacket.getTAR()) && previous_fr._commandPacket.getKeyset() == fr._commandPacket.getKeyset()) {
                        System.out.printf(" %s", HexToolkit.toString(fr._responsePacket.getBytes()));
                    } else {
                        System.out.printf("\n%-6s %6s %s", HexToolkit.toString(fr._commandPacket.getTAR()), fr._commandPacket.getKeyset(), HexToolkit.toString(fr._responsePacket.getBytes()));
                    }
                    previous_fr = fr;
                }
                System.out.println();
            }
            if (fuzzer.wibCommandExecuted.size() > 0) {
                System.out.println();
                System.out.println("The following TARs/keysets accepted and executed a WIB request without any security:");
                System.out.printf("%-6s %6s %s\n", "TAR", "keyset", "Response packets");
                fuzzer.wibCommandExecuted = new ArrayList<>(new HashSet(fuzzer.wibCommandExecuted)); // make the results unique
                Collections.sort(fuzzer.wibCommandExecuted, new FuzzerResultComparator()); // sort them by TAR
                FuzzerResult previous_fr = null;
                for (FuzzerResult fr : fuzzer.wibCommandExecuted) {
                    if (null != previous_fr && Arrays.equals(previous_fr._commandPacket.getTAR(), fr._commandPacket.getTAR()) && previous_fr._commandPacket.getKeyset() == fr._commandPacket.getKeyset()) {
                        System.out.printf(" %s", HexToolkit.toString(fr._responsePacket.getBytes()));
                    } else {
                        System.out.printf("\n%-6s %6s %s", HexToolkit.toString(fr._commandPacket.getTAR()), fr._commandPacket.getKeyset(), HexToolkit.toString(fr._responsePacket.getBytes()));
                    }
                    previous_fr = fr;
                }
                System.out.println();
            }
            if (fuzzer.satCommandExecuted.size() > 0) {
                System.out.println();
                System.out.println("The following TARs/keysets accepted and executed a S@T request without any security:");
                System.out.printf("%-6s %6s %s\n", "TAR", "keyset", "Response packets");
                fuzzer.satCommandExecuted = new ArrayList<>(new HashSet(fuzzer.satCommandExecuted)); // make the results unique
                Collections.sort(fuzzer.satCommandExecuted, new FuzzerResultComparator()); // sort them by TAR
                FuzzerResult previous_fr = null;
                for (FuzzerResult fr : fuzzer.satCommandExecuted) {
                    if (null != previous_fr && Arrays.equals(previous_fr._commandPacket.getTAR(), fr._commandPacket.getTAR()) && previous_fr._commandPacket.getKeyset() == fr._commandPacket.getKeyset()) {
                        System.out.printf(" %s", HexToolkit.toString(fr._responsePacket.getBytes()));
                    } else {
                        System.out.printf("\n%-6s %6s %s", HexToolkit.toString(fr._commandPacket.getTAR()), fr._commandPacket.getKeyset(), HexToolkit.toString(fr._responsePacket.getBytes()));
                    }
                    previous_fr = fr;
                }
                System.out.println();
            }
            if (fuzzer.decryptionOracleResponses.size() > 0) {
                System.out.println();
                System.out.println("The following TARs/keysets act as a decryption oracle (decrypted counter value):");
                System.out.printf("%-6s %6s %s\n", "TAR", "keyset", "Response packets");
                fuzzer.decryptionOracleResponses = new ArrayList<>(new HashSet(fuzzer.decryptionOracleResponses)); // make the results unique
                Collections.sort(fuzzer.decryptionOracleResponses, new FuzzerResultComparator()); // sort them by TAR
                FuzzerResult previous_fr = null;
                for (FuzzerResult fr : fuzzer.decryptionOracleResponses) {
                    if (null != previous_fr && Arrays.equals(previous_fr._commandPacket.getTAR(), fr._commandPacket.getTAR()) && previous_fr._commandPacket.getKeyset() == fr._commandPacket.getKeyset()) {
                        System.out.printf(" %s", HexToolkit.toString(fr._responsePacket.getBytes()));
                    } else {
                        System.out.printf("\n%-6s %6s %s", HexToolkit.toString(fr._commandPacket.getTAR()), fr._commandPacket.getKeyset(), HexToolkit.toString(fr._responsePacket.getBytes()));
                    }
                    previous_fr = fr;
                }
                System.out.println();
            }
        } else {
            System.out.println("\033[92mSIMTester hasn't detected any weaknesses it tests for.\033[0m");
        }
        System.out.println();
    }

    private static void listAllCards() throws Exception {

        List<CardTerminal> terminals = TerminalFactory.getDefault().terminals().list();

        System.out.println("Terminals connected: " + terminals.size());
        for (CardTerminal terminal : terminals) {
            System.out.println(terminal);
        }
        System.out.println();

        int i = 0;
        for (CardTerminal terminal : terminals) {
            try {
                terminal.connect("T=0");
                ChannelHandler.getInstance(i, null);
                System.out.println("IDX: " + i + ", ICCID = " + CommonFileReader.readICCID());
            } catch (CardException ignored) {
            } finally {
                i++;
            }

        }
    }

    private static String checkTerminalFactory(String terminalFactoryName) {
        switch (terminalFactoryName) {
            case "PCSC":
                System.out.println("Using pcscd daemon to get a SIM card reader");
                return null;
            case "OsmocomBB":
                System.out.println("Using OsmocomBB mobile as SIM card reader");
                return "OsmoTerminalFactory";
            default:
                throw new IllegalArgumentException("Terminal factory (-tf) has to be either PCSC or OsmocomBB!");
        }
    }

    private static void handleOptions(String args[]) throws Exception {
        CommandLineParser parser = new GnuParser();
        Options options = new Options();

        options.addOption("h", "help", false, "this help");
        options.addOption("v", "version", false, "version of SIMTester");
        options.addOption("2g", "2g-cmds", false, "Use 2G APDU format only");
        options.addOption("qf", "quick-fuzz", false, "Use quick fuzzing instead (only probes statistically best keysets (1-6) w/ best 4 most successful fuzzers)");
        options.addOption("poke", "poke-fuzz", false, "Fuzzing technique, only 'poke' the card, same as quick-fuzz, but only probes 3 TARs (000000, B00001, B00010)");
        options.addOption("d", "debug", false, "Enables debug messages");
        options.addOption("la", "list-all", false, "Try to connect to all readers and show info about cards in them");
        options.addOption("of", "ota-fuzz", false, "Fuzz OTA passthrough (PID, DCS, UDHI, IEI/CPH)");
        options.addOption("ofbf", "ota-fuzz-bruteforce", false, "Use 0-255 values for both PID and DCS, without this options only most common values are used.");
        options.addOption("nl", "no-logging", false, "Skip the CSV logging");
        options.addOption("sp", "skip-pin", false, "Skips the PIN1/CHV1");
        options.addOption("sdr", "sms-deliver-report", false, "Use SMS-DELIVER-REPORT instead of SMS-SUBMIT for PoR");
        options.addOption("st", "scan-tars", false, "Scans all possbile TAR values (takes time!), starting TAR can be chosen with -t option (first -t (TAR) option will be used)");
        options.addOption("str", "scan-tars-range", false, "Scans TAR values but only pre-specified ranges that usually contain most of the TARs");
        options.addOption("stbs", "scan-tars-be-smart", false, "Try being smart while scanning TARs - scan a few random TARs to determine false response");
        options.addOption("stre", "scan-tars-regexp", true, "Specify a regexp to match on responses to determine a false response");
        options.addOption("sa", "scan-apdu", false, "Scans all possible CLA (and INS w/ -sal2) values to discover valid APDU commands");
        options.addOption("sal2", "scan-apdu-level2", false, "Will also scan for INS for each CLA for both terminal and OTA APDU scanning (use with option -sa)");
        options.addOption("sfb", "scan-files-break", false, "Use with -sf, stop scanning directory when the count returned by Select APDU matched count of found files");
        options.addOption("sffs", "scan-files-follow-standard", false, "Use with -sf, only search for IDs that are standardized, eg. 3rd level files only between 4F00 and 4FFF etc.");
        options.addOption("sf", "scan-files", false, "Scans files on the SIM, starts at MF (0x3F00)");
        options.addOption("kic", "kic", true, "Overwrites KIC byte in all fuzzer messages to a custom value");
        options.addOption("kid", "kid", true, "Overwrites KID byte in all fuzzer messages to a custom value");
        options.addOption("spi1", "spi1", true, "Overwrites SPI1 byte in all fuzzer messages to a custom value");
        options.addOption("spi2", "spi2", true, "Overwrites SPI2 byte in all fuzzer messages to a custom value");
        options.addOption("vp", "verify-pin", true, "Verifies the PIN1/CHV1");
        options.addOption("dp", "disable-pin", true, "Disabled the PIN1/CHV1");
        options.addOption("ri", "reader-index", true, "SIM card reader index (PCSC), OsmocomBB only supports 1 reader (index=0, default)");
        options.addOption("tf", "terminal-factory", true, "Terminal factory/type, either PCSC or OsmocomBB");
        options.addOption("gsmmap", "gsmmap", false, "Automatically upload data to gsmmap.org");
        options.addOption(OptionBuilder.withLongOpt("tar").withDescription("TAR(s) to be tested, prefixed with a type, eg. 'RFM:B00010' or 'RAM:000000'").withValueSeparator(' ').hasArgs().withArgName("tar").create("t"));
        options.addOption(OptionBuilder.withLongOpt("keyset").withDescription("keyset(s) to be tested").withValueSeparator(' ').hasArgs().withArgName("keysets").create("k"));
        options.addOption(OptionBuilder.withLongOpt("fuzzer").withDescription("fuzzer(s) to be used").withValueSeparator(' ').hasArgs().withArgName("fuzzers").create("f"));
        options.addOption(OptionBuilder.withLongOpt("sfrv").withDescription("File scanning: Add a file ID(s) to reserved values for file scanning (will be skipped).").withValueSeparator(' ').hasArgs().withArgName("sfrv").create("sfrv"));

        try {
            // parse the command line arguments
            cmdline = parser.parse(options, args);

            if (cmdline.hasOption("h")) {
                printUsage(options);
            }

            if (cmdline.hasOption("v")) {
                System.out.println(version);
                System.out.println(SIMLibrary.version);
                System.exit(0);
            }

            if (cmdline.hasOption("d")) {
                DEBUG = true;
                Debug.DEBUG = true;
            }

            if (cmdline.hasOption("nl")) {
                _logging = false;
            }

            if (cmdline.hasOption("2g")) {
                SIMLibrary.third_gen_apdu = false;
            }

            if (cmdline.hasOption("la")) {
                listAllCards();
                System.exit(0);
            }

            if (cmdline.hasOption("tf") && cmdline.hasOption("ri")) {
                ChannelHandler.getInstance(Integer.parseInt(cmdline.getOptionValue("ri")), checkTerminalFactory(cmdline.getOptionValue("tf")));
            } else if (cmdline.hasOption("tf")) {
                ChannelHandler.getInstance(0, checkTerminalFactory(cmdline.getOptionValue("tf")));
            } else if (cmdline.hasOption("ri")) {
                ChannelHandler.getInstance(Integer.parseInt(cmdline.getOptionValue("ri")), checkTerminalFactory("PCSC")); // PCSC is default
            } else {
                ChannelHandler.getInstance(0, checkTerminalFactory("PCSC")); // PCSC is default, reader index = 0 is default
            }

            ChannelHandler.getInstance().reset();
            if (SIMLibrary.third_gen_apdu) { // auto-detect if card supports 3G APDUs
                try {
                    ResponseAPDU response = FileManagement.selectFileById(new byte[]{(byte) 0x3F, (byte) 0x00});
                    if ((short) response.getSW() != (short) 0x9000) { // 3G failed
                        if ((short) response.getSW() == (short) 0x6E00) {
                            // Class not supported
                            System.err.println("\033[96m" + "3G APDU FAILED, this card does NOT support 3G, falling back to 2G and auto-retrying..\033[0m");
                            SIMLibrary.third_gen_apdu = false;
                        } else {
                            // Other error, so not a good sign, you have to investigate...
                            System.err.println("\033[96m" + "3G APDU FAILED, " + String.format("%04X", response.getSW()) + " returned. Please investigate, fix and try again...\033[0m");
                            System.exit(1);
                        }
                    }
                    if (DEBUG) {
                        System.out.println(LoggingUtils.formatDebugMessage("3G auto-detect returned: " + HexToolkit.toString(response.getBytes())));
                    }
                } catch (CardException e) {
                    e.printStackTrace(System.err);
                    System.exit(1);
                }
            }
            ChannelHandler.getInstance().reset();

            if (cmdline.hasOption("dp")) {
                System.out.println("Disabling PIN1/CHV1..");
                Auth.disableCHV(1, cmdline.getOptionValue("dp"));
            }
            if (cmdline.hasOption("vp")) {
                System.out.println("Verifying PIN1/CHV1..");
                Auth.verifyCHV(1, cmdline.getOptionValue("vp"));
            }
            if (cmdline.hasOption("sp")) {
                System.out.println("Skipping PIN1/CHV1, trying the best we can without it!");
                _skipPin = true;
            }

            if (cmdline.hasOption("t")) {
                List<String> cmdTARs = Arrays.asList(cmdline.getOptionValues("t"));
                customTARs = new ArrayList<>();
                for (String cmdTAR : cmdTARs) {
                    if (cmdTAR.startsWith("RFM") || cmdTAR.startsWith("RAM") || cmdTAR.startsWith("WIB") || cmdTAR.startsWith("SAT")) {
                        customTARs.add(cmdTAR);
                    } else {
                        System.err.println(LoggingUtils.formatDebugMessage("Each TAR has to match a type (RFM, RAM, WIB, SAT), this one does not: " + cmdTAR));
                    }
                }
                if (customTARs.isEmpty()) {
                    customTARs = null;
                }
            }

            if (cmdline.hasOption("k")) {
                customKeysets = getIntegerArray(Arrays.asList(cmdline.getOptionValues("k")));
            }

            if (cmdline.hasOption("sfb") && !cmdline.hasOption("sf")) {
                System.err.println(LoggingUtils.formatDebugMessage("Option -sfb (scan-files-break) has to be used along with -sf, exiting!"));
                System.exit(1);
            }

            if (cmdline.hasOption("sffs") && !cmdline.hasOption("sf")) {
                System.err.println(LoggingUtils.formatDebugMessage("Option -sffs (scan-files-follow-standard) has to be used along with -sf, exiting!"));
                System.exit(1);
            }

            if (cmdline.hasOption("sfrv")) {
                FileScanner.userDefinedReservedIDs = Arrays.asList(cmdline.getOptionValues("sfrv"));
            }

            if (cmdline.hasOption("sf")) {
                readBasicInfo();
                boolean breakAfterCount = false;
                boolean lazyScan = false;

                if (cmdline.hasOption("sfb")) {
                    breakAfterCount = true;
                }

                if (cmdline.hasOption("sffs")) {
                    lazyScan = true;
                }

                CSVWriter _writer = new CSVWriter(ICCID, "FILE", _logging);
                _writer.writeBasicInfo(ATR, ICCID, IMSI, MSISDN, EF_MANUAREA, EF_DIR, AUTH, AppDeSelect);
                FileScanner.scanSim(breakAfterCount, lazyScan, _writer);

                _writer.unhideFile();
                System.out.println("done scanning files, exiting..");
                System.exit(0);
            }

            if (cmdline.hasOption("st") || cmdline.hasOption("str")) {
                action = "TAR";
            }

            if (cmdline.hasOption("sa")) {
                action = "APDU";
            }

            if (cmdline.hasOption("of")) {
                action = "OTA";
            }

            if (cmdline.hasOption("qf")) {
                _fuzzingLevel = "QUICK";
            }

            if (cmdline.hasOption("poke")) {
                _fuzzingLevel = "POKE";
            }

            if (cmdline.hasOption("gsmmap")) {
                _gsmmap_upload = true;
            }

            if (cmdline.hasOption("f")) {
                customFuzzers = getIntegerArray(Arrays.asList(cmdline.getOptionValues("f")));
            }

            if (cmdline.hasOption("sdr")) {
                Fuzzer.use_sms_submit = false; // this applies for both fuzzing and OTA fuzzing
                TARScanner.use_sms_submit = false;
            }

            if (cmdline.hasOption("kic")) {
                Fuzzer.KIC = HexToolkit.fromStringToSingleByte(cmdline.getOptionValue("kic").substring(0, 1));
            }

            if (cmdline.hasOption("kid")) {
                Fuzzer.KID = HexToolkit.fromStringToSingleByte(cmdline.getOptionValue("kid").substring(0, 1));
            }

            if (cmdline.hasOption("spi1")) {
                Fuzzer.SPI1 = HexToolkit.fromStringToSingleByte(cmdline.getOptionValue("spi1").substring(0, 2));
            }

            if (cmdline.hasOption("spi2")) {
                Fuzzer.SPI2 = HexToolkit.fromStringToSingleByte(cmdline.getOptionValue("spi2").substring(0, 2));
            }

        } catch (ParseException exp) {
            System.err.println(LoggingUtils.formatDebugMessage("Unexpected exception: " + exp.getMessage()));
            System.exit(1);
        }
    }

    private static void printUsage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("SIMTester.jar <arguments>", options);
        System.exit(1);
    }

    private static ArrayList<Integer> getIntegerArray(List<String> stringArray) {
        ArrayList<Integer> result = new ArrayList<>();
        for (String stringValue : stringArray) {
            try {
                //Convert String to Integer, and store it into integer array list.
                result.add(Integer.parseInt(stringValue));
            } catch (NumberFormatException nfe) {
                System.err.println(LoggingUtils.formatDebugMessage("Parsing failed! \"" + stringValue + "\" can not be an converted to an integer, try again!"));
                System.exit(1);
            }
        }
        return result;
    }

    public static String getVersion() {
        return version;
    }
}
