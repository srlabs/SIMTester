package de.srlabs.simtester;

import de.srlabs.simlib.Auth;
import de.srlabs.simlib.AutoTerminalProfile;
import de.srlabs.simlib.ChannelHandler;
import de.srlabs.simlib.CommonFileReader;
import de.srlabs.simlib.Debug;
import de.srlabs.simlib.HexToolkit;
import de.srlabs.simlib.LoggingUtils;
import de.srlabs.simlib.SIMLibrary;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import javax.smartcardio.CardException;
import javax.smartcardio.ResponseAPDU;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class Main {

    public static boolean DEBUG = false;
    private static CommandLine cmdline;
    private static String action = "";
    private static List<String> TARs = FuzzerFactory.defaultTARs;
    private static List<Integer> customFuzzers = null;
    private static List<Integer> customKeysets = null;
    private static List<String> customTARs = null;
    private static String _fuzzingLevel = "FULL";
    private static boolean _skipPin = false;
    public static boolean _logging = true;
    public static String ATR = null;
    public static String ICCID = null;
    public static String IMSI = null;
    public static String MSISDN = null;
    public static String EF_MANUAREA = null;
    public static String EF_DIR = null;
    public static String AppDeSelect = null;
    private static final String version = "SIMTester v1.4.6, 2013-12-25";
    private static Fuzzer _fuzzer = null;
    private static CSVWriter _writer = null;

    public static void main(String[] args) throws Exception {

        System.out.println();
        System.out.println("########################################");
        System.out.println("  " + version);
        System.out.println("  Lukas Kuzmiak (lukas@srlabs.de)       ");
        System.out.println("  Security Research Labs, Berlin, 2013  ");
        System.out.println("########################################");
        System.out.println();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {

                System.out.println();
                System.out.println("Graceful shutdown initiated. Trying to close all open channels. Please wait... !");

                if (null != _fuzzer) {
                    _fuzzer.interrupt();
                    try {
                        _fuzzer.join(); // wait for all actions in thread to finish
                    } catch (InterruptedException e) {
                    }
                }

                try {
                    ChannelHandler.closeChannel(); // FIXME: this get hung up if layer1 connection does not answer anymore and you have to kill -9 <java>
                } catch (CardException e) {
                }

                if (null != _fuzzer) {
                    printSummary(_fuzzer);
                }
                if (null != _writer) {
                    if (!_writer.unhideFile()) {
                        System.err.println(LoggingUtils.formatDebugMessage("Unable to unhide file " + _writer.getFileName() + ", make sure you rename it so it does NOT start with a dot to get processed!"));
                    }
                }
            }
        });

        handleOptions(args);

        switch (action) {
            case "TAR":
                readBasicInfo();
                _writer = new CSVWriter(ICCID, "TAR", _logging);
                _writer.writeBasicInfo(ATR, ICCID, IMSI, EF_MANUAREA, EF_DIR, AppDeSelect);

                int keyset = 1;
                if (cmdline.hasOption("k") && !customKeysets.isEmpty()) {
                    keyset = customKeysets.get(0);
                }

                if (cmdline.hasOption("str")) {
                    TARScanner.scanRangesOfTARs(keyset, _writer);
                } else {
                    String startingTAR = "000000";

                    if (cmdline.hasOption("t") && !customTARs.isEmpty()) {
                        String[] split_TAR = customTARs.get(0).split(":");
                        startingTAR = split_TAR[1];
                    }

                    TARScanner.scanAllTARs(startingTAR, keyset, _writer);
                }

                System.out.println("done scanning TARs, exiting..");
                break;
            case "APDU":
                readBasicInfo();
                _writer = new CSVWriter(ICCID, "APDU", _logging);
                _writer.writeBasicInfo(ATR, ICCID, IMSI, EF_MANUAREA, EF_DIR, AppDeSelect);
                APDUScanner.run(null, _writer, false, true);
                break;
            default:
                readBasicInfo();
                _writer = new CSVWriter(ICCID, "FUZZ", _logging);
                _writer.writeBasicInfo(ATR, ICCID, IMSI, EF_MANUAREA, EF_DIR, AppDeSelect);
                fuzz();
        }
    }

    private static void readBasicInfo() throws Exception {
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
        System.out.println("ICCID: " + ICCID);
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

        EF_MANUAREA = CommonFileReader.readMANUAREA();
        System.out.println("EF_MANUAREA: " + EF_MANUAREA);

        EF_DIR = CommonFileReader.readDIR();
        System.out.println("EF_DIR: " + EF_DIR);

        ResponseAPDU deselectResponse = Fuzzer.applicationDeSelect();
        AppDeSelect = HexToolkit.toString(deselectResponse.getBytes());
        System.out.println("AppDeSelect: " + AppDeSelect);

        ChannelHandler.getInstance().reset();
    }

    private static void fuzz() throws Exception {
        if (AutoTerminalProfile.autoTerminalProfile()) {
            if (DEBUG) {
                System.out.println(LoggingUtils.formatDebugMessage("Automatic Terminal profile initialization SUCCESSFUL!"));
            }
        } else {
            if (DEBUG) {
                System.out.println(LoggingUtils.formatDebugMessage("Automatic Terminal profile initialization FAILED!"));
            }
        }

        System.out.println();
        System.out.println("Starting fuzzing!");
        System.out.println("Fuzzing level: " + _fuzzingLevel);
        System.out.println();
        System.out.println("TAR values to be fuzzed: " + TARs);
        System.out.println();

        ArrayList<FuzzerData> fuzzers = new ArrayList<>();
        List keysets = null;
        switch (_fuzzingLevel) {
            case "FULL":
                fuzzers.addAll(FuzzerFactory.getAllFuzzers());
                keysets = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15));
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
                if (oneFuzzer < 1 || oneFuzzer > FuzzerFactory.getAmountOfFuzzers()) {
                    System.err.println(LoggingUtils.formatDebugMessage("Fuzzer(s) you specified does NOT exist, use values 1-" + FuzzerFactory.getAmountOfFuzzers() + "!"));
                    System.exit(1);
                }
                fuzzers.add(FuzzerFactory.getFuzzer(oneFuzzer));
            }
        }

        if (null != customKeysets) {
            keysets = new ArrayList<>();
            for (Integer oneKeyset : customKeysets) {
                keysets.add(oneKeyset);
            }
        }

        if (null != customTARs) {
            TARs = new ArrayList<>();
            for (String oneTAR : customTARs) {
                TARs.add(oneTAR);
            }
        }

        _fuzzer = new Fuzzer(_writer, TARs, keysets, fuzzers);
        _fuzzer.start();
        _fuzzer.join();
    }

    private static void printSummary(Fuzzer fuzzer) {
        System.out.println();
        if (fuzzer.isThereAWeaknessFound()) {
            if (fuzzer.unprotectedTARsResponses.size() > 0) {
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
        } else {
            System.out.println("\033[92mSIMTester hasn't detected any weaknesses it tests for.\033[0m");
        }
        System.out.println();
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
        options.addOption("qf", "quick-fuzz", false, "Use quick fuzzing instead (only probes statistically best keysets (1-6) w/ best 4 most successful fuzzers)");
        options.addOption("poke", "poke-fuzz", false, "Fuzzing technique, only 'poke' the card, same as quick-fuzz, but only probes 3 TARs (000000, B00001, B00010)");
        options.addOption("d", "debug", false, "Enables debug messages");
        options.addOption("sp", "skip-pin", false, "Skips the PIN1/CHV1");
        options.addOption("st", "scan-tars", false, "Scans all possbile TAR values (takes time!), starting TAR can be chosen with -t option (first -t (TAR) option will be used)");
        options.addOption("str", "scan-tars-range", false, "Scans TAR values but only pre-specified ranges that usually contain most of the TARs");
        options.addOption("sa", "scan-apdu", false, "Scans all possible CLA and INS values to discover valid APDU commands");
        options.addOption("vp", "verify-pin", true, "Verifies the PIN1/CHV1");
        options.addOption("dp", "disable-pin", true, "Disabled the PIN1/CHV1");
        options.addOption("ri", "reader-index", true, "SIM card reader index (PCSC), OsmocomBB only supports 1 reader (index=0, default)");
        options.addOption("tf", "terminal-factory", true, "Terminal factory/type, either PCSC or OsmocomBB");
        options.addOption(OptionBuilder.withLongOpt("tar").withDescription("TAR(s) to be tested, prefixed with a type, eg. 'RFM:B00010' or 'RAM:000000'").withValueSeparator(' ').hasArgs().withArgName("tar").create("t"));
        options.addOption(OptionBuilder.withLongOpt("keyset").withDescription("keyset(s) to be tested").withValueSeparator(' ').hasArgs().withArgName("keysets").create("k"));
        options.addOption(OptionBuilder.withLongOpt("fuzzer").withDescription("fuzzer(s) to be used").withValueSeparator(' ').hasArgs().withArgName("fuzzers").create("f"));

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

            if (cmdline.hasOption("tf")) {
                if (cmdline.hasOption("ri")) {
                    ChannelHandler.getInstance(Integer.parseInt(cmdline.getOptionValue("ri")), checkTerminalFactory(cmdline.getOptionValue("tf")));
                } else {
                    ChannelHandler.getInstance(0, checkTerminalFactory(cmdline.getOptionValue("tf")));
                }
            } else {
                ChannelHandler.getInstance(0, checkTerminalFactory("PCSC")); // PCSC is default
            }

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
                    if (cmdTAR.startsWith("RFM") || cmdTAR.startsWith("RAM")) {
                        customTARs.add(cmdTAR);
                    } else {
                        System.err.println(LoggingUtils.formatDebugMessage("Each TAR has to match a type (RFM, RAM), this one does not: " + cmdTAR));
                    }
                }
                if (customTARs.isEmpty()) {
                    customTARs = null;
                }

            }

            if (cmdline.hasOption("k")) {
                customKeysets = getIntegerArray(Arrays.asList(cmdline.getOptionValues("k")));
            }

            if (cmdline.hasOption("st") || cmdline.hasOption("str")) {
                action = "TAR";
            }

            if (cmdline.hasOption("sa")) {
                action = "APDU";
                System.out.println("done scanning APDUs, exiting..");
            }

            if (cmdline.hasOption("qf")) {
                _fuzzingLevel = "QUICK";
            }

            if (cmdline.hasOption("poke")) {
                _fuzzingLevel = "POKE";
            }

            if (cmdline.hasOption("f")) {
                customFuzzers = getIntegerArray(Arrays.asList(cmdline.getOptionValues("f")));
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
}
