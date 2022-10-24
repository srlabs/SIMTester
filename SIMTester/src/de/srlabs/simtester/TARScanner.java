package de.srlabs.simtester;

import de.srlabs.simlib.AutoTerminalProfile;
import de.srlabs.simlib.ByteArray;
import de.srlabs.simlib.CommandPacket;
import de.srlabs.simlib.Debug;
import de.srlabs.simlib.Helpers;
import de.srlabs.simlib.HexToolkit;
import de.srlabs.simlib.LoggingUtils;
import de.srlabs.simlib.OTASMS;
import de.srlabs.simlib.Range;
import de.srlabs.simlib.ResponsePacket;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.smartcardio.CardException;
import javax.smartcardio.ResponseAPDU;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import org.paukov.combinatorics.Factory;
import org.paukov.combinatorics.Generator;
import org.paukov.combinatorics.ICombinatoricsVector;

public class TARScanner extends Thread {

    private final static boolean LOCAL_DEBUG = false;
    private final static boolean DEBUG = Debug.DEBUG || LOCAL_DEBUG;
    // input variables
    public static boolean use_sms_submit = true;
    // required variables
    private final static int HIGHEST_TAR = 16777215; // this is 0xFFFFFF which is the highest possible TAR value
    private static int _keyset;
    private static CSVWriter _writer = null;
    private static String _mode = null;
    private static String _startingTAR = null;
    // tryBeingSmart variables
    private static boolean _try_being_smart = false;
    private final static int _smart_count = 20;
    private final static List<ByteArray> skip_responses = new ArrayList();
    // regexp-based skipping variables
    private static Pattern _regexp_to_match_response_pattern = null;
    // premature exit variables
    private static String _status_last_TAR_scanned = null;
    private static int _status_remaining_TARs = 0;

    public TARScanner(String mode, int keyset, CSVWriter writer) {
        this(mode, keyset, writer, false, null);
    }

    public TARScanner(String mode, int keyset, CSVWriter writer, boolean try_being_smart, String regexp_to_match_response) {

        if ("scanAllTARs".equals(mode) || "scanRangesOfTARs".equals(mode)) {
            _mode = mode;
        } else {
            throw new IllegalArgumentException("Unsupported mode");
        }

        if (null == writer) {
            throw new IllegalArgumentException("writer cannot be null!");
        } else {
            _writer = writer;
        }

        if (keyset < 0 || keyset > 15) {
            throw new IllegalArgumentException("invalid keyset!");
        } else {
            _keyset = keyset;
        }

        _try_being_smart = try_being_smart;

        if (null != regexp_to_match_response) {
            try {
                _regexp_to_match_response_pattern = Pattern.compile(regexp_to_match_response);
            } catch (PatternSyntaxException e) {
                System.err.println("Unable to compile Pattern from input specified (" + regexp_to_match_response + "), exiting..");
                e.printStackTrace(System.err);
                System.exit(1);
            }
        }
    }

    public void setStartingTAR(String startingTAR) {
        if (!startingTAR.matches("[0-9A-F]+") || startingTAR.length() != 6) {
            throw new IllegalArgumentException("TAR value has to be hexadecimal value, 3 bytes long, yours is NOT! value = " + startingTAR);
        } else {
            _startingTAR = startingTAR;
        }
    }

    @Override
    public void run() {
        try {
            switch (_mode) {
                case "scanAllTARs":
                    scanAllTARs();
                    break;
                case "scanRangesOfTARs":
                    scanRangesOfTARs();
                    break;
            }
        } catch (Exception ex) { // lol what a mess
            Thread t = Thread.currentThread();
            t.getUncaughtExceptionHandler().uncaughtException(t, ex);
        }
    }

    private void initScan(int amountOfTARs) throws CardException {

        if (AutoTerminalProfile.autoTerminalProfile()) {
            if (DEBUG) {
                System.out.println(LoggingUtils.formatDebugMessage("Automatic Terminal profile initialization SUCCESSFUL!"));
            }
        } else {
            if (DEBUG) {
                System.out.println(LoggingUtils.formatDebugMessage("Automatic Terminal profile initialization FAILED!"));
            }
        }

        if (_try_being_smart) {
            System.out.println();
            tryBeingSmart();
        }

        System.out.println();
        System.out.println("Starting TAR scanning (" + _mode + "), going to go over " + amountOfTARs + " TARs, go get a (few) coffee(s) ..");
        System.out.println();

        _writer.writeRawLine("# " + _mode + "," + amountOfTARs);
    }

    public void scanExit() {
        if (_status_remaining_TARs > 0) {
            _writer.writeRawLine("# exit," + _mode + ",last_scanned:" + _status_last_TAR_scanned + ",remaining:" + _status_remaining_TARs);
        }
    }

    private void scanAllTARs() throws Exception {
        if (null == _startingTAR) {
            _startingTAR = "000000";
        }

        int startingTAR_int = Integer.valueOf(_startingTAR, 16);
        int loopUntil = HIGHEST_TAR - startingTAR_int;

        initScan(loopUntil);

        long startTime = System.currentTimeMillis();

        int i = 0;
        boolean hasCardException = false;   // Allow just one single CardException
        while (i <= loopUntil && !Thread.currentThread().isInterrupted()) {
            int int_TAR = i + startingTAR_int;
            byte[] currentTAR = new byte[]{(byte) (int_TAR >>> 16), (byte) (int_TAR >>> 8), (byte) (int_TAR)};
            if ((i == loopUntil) || (i % 100) == 0) {
                long currentTime = System.currentTimeMillis();
                if (i != 0) {
                    long timePassed = currentTime - startTime;
                    long diffTime = (long) (((double) timePassed / (double) i) * (loopUntil - i));
                    Duration duration = DatatypeFactory.newInstance().newDuration(diffTime);
                    System.out.printf("Processing TAR: %s, already processed: %d TARs, to process: %d TARs, approximate remaining time: %d days, %d hours, %d minutes, %d seconds\n", HexToolkit.toString(currentTAR), i, (loopUntil - i), duration.getDays(), duration.getHours(), duration.getMinutes(), duration.getSeconds());
                }
            }

            CommandPacket cp = new CommandPacket();
            ResponseAPDU response = testTAR(currentTAR, cp);
//            analyseResponse(currentTAR, cp, response);

            try {
                analyseResponse(currentTAR, cp, response);
                hasCardException = false;
            } catch (CardException e) {
                if (!hasCardException) {
                    System.err.println(e.getMessage());
                    hasCardException = true;
                } else {
                    throw e;
                }

            }

            _status_last_TAR_scanned = HexToolkit.toString(currentTAR);
            _status_remaining_TARs = (loopUntil - i - 1); // -1 as the TAR was already processed

            i++;
//            break;
        }
    }

    private void scanRangesOfTARs() throws Exception {
        ArrayList<ByteArray> TARList = prepareTARlist();
        int loopUntil = TARList.size();
        int startIndex = 0;

        if (null != _startingTAR) {
            if (TARList.contains(new ByteArray(_startingTAR))) {
                startIndex = TARList.indexOf(new ByteArray(_startingTAR));
            } else {
                throw new IllegalArgumentException("Starting TAR " + _startingTAR + " is not contained in range generated, specify a valid starting TAR");
            }
        }

        initScan(loopUntil);

        long startTime = System.currentTimeMillis();

        for (int i = startIndex; i < loopUntil && !Thread.currentThread().isInterrupted(); i++) {
            byte[] currentTAR = TARList.get(i).getData();
            if (i == (loopUntil - 1) || (i % 100) == 0) {
                long currentTime = System.currentTimeMillis();
                if (i != 0) {
                    long timePassed = currentTime - startTime;
                    long diffTime = (long) (((double) timePassed / (double) i) * (loopUntil - i));
                    Duration duration = DatatypeFactory.newInstance().newDuration(diffTime);
                    System.out.printf("Processing TAR: %s, already processed: %d TARs, to process: %d TARs, approximate remaining time: %d days, %d hours, %d minutes, %d seconds\n", HexToolkit.toString(currentTAR), i, (loopUntil - i), duration.getDays(), duration.getHours(), duration.getMinutes(), duration.getSeconds());
                }
            }

            CommandPacket cp = new CommandPacket();
            ResponseAPDU response = testTAR(currentTAR, cp);
            analyseResponse(currentTAR, cp, response);

            _status_last_TAR_scanned = HexToolkit.toString(currentTAR);
            _status_remaining_TARs = (loopUntil - i - 1); // -1 as the TAR was already processed
        }

    }

    private ResponseAPDU testTAR(byte[] TAR, CommandPacket cp) throws CardException {

        cp.setKeyset(_keyset);
        cp.setCounterManegement(CommandPacket.CNTR_NO_CNTR_AVAILABLE);
        cp.setCounter(1);
        cp.setPoR(true); // request PoR
        cp.setPoRSecurity(CommandPacket.POR_SECURITY_CC); // sign the PoR packet
        if (use_sms_submit) {
            cp.setPoRMode(CommandPacket.POR_MODE_SMS_SUBMIT);
        } else {
            cp.setPoRMode(CommandPacket.POR_MODE_SMS_DELIVER_REPORT);
        }
        cp.setUserData(HexToolkit.fromString("A0A40000023F00"));

        OTASMS otasms = new OTASMS();

        cp.setTAR(TAR);
        otasms.setCommandPacket(cp);
        ResponseAPDU response = otasms.send();

        if (DEBUG) {
            System.out.println(LoggingUtils.formatDebugMessage("ResponseAPDU bytes: " + HexToolkit.toString(response.getBytes())));
        }

        return response;

    }

    private void analyseResponse(byte[] TAR, CommandPacket command, ResponseAPDU response) throws CardException {

        ResponseAPDU handled_response = Helpers.handleSIMResponse(response, false); // don't show summary
        ByteArray currentResponse = new ByteArray(handled_response.getBytes());

        if (null != _regexp_to_match_response_pattern) {
            Matcher matcher = _regexp_to_match_response_pattern.matcher(currentResponse.getStringData());
            if (matcher.find()) {
                if (DEBUG) {
                    System.out.println(LoggingUtils.formatDebugMessage("Response " + currentResponse.getStringData() + " skipped as it matches the regular expression pattern specified."));
                }
                return;
            }

        }

        byte[] possible_rp = ResponsePacket.Helpers.findResponsePacket(handled_response.getData());

        if (null != possible_rp) {
            ResponsePacket rp = new ResponsePacket();

            try {
                rp.parse(possible_rp);
            } catch (ParseException e) {
                System.err.println("Parse exception while parsing response packet, skipping it! details:");
                e.printStackTrace(System.err);
                return;
            } catch (ArrayIndexOutOfBoundsException e) {
                throw new CardException(e.getMessage());
            }

            byte status_code = rp.getStatusCode();

            if (_try_being_smart) {
                if (skip_responses.contains(new ByteArray(new byte[]{rp.getStatusCode()}))) {
                    if (DEBUG) {
                        System.out.println(LoggingUtils.formatDebugMessage("Response " + currentResponse.getStringData() + " skipped as it's being considered a false response (by tryBeingSmart)."));
                    }
                    return;
                }
            }

            if (status_code != 9) {
                System.out.println("GOT VALID TAR!! (" + HexToolkit.toString(TAR) + "), PoR status code: " + HexToolkit.toString(status_code));
                _writer.writeLine(HexToolkit.toString(TAR), command.getBytes(), handled_response.getBytes());
            }
        } else {

            if (_try_being_smart) {
                if (skip_responses.contains(currentResponse)) {
                    if (DEBUG) {
                        System.out.println(LoggingUtils.formatDebugMessage("Response " + currentResponse.getStringData() + " skipped as it's being considered a false response (by tryBeingSmart)."));
                    }
                    return;
                }
            }

            System.out.println("RESPONSE OTHER THAN ERROR!! (" + HexToolkit.toString(TAR) + "), response: " + currentResponse.getStringData());
            _writer.writeLine(HexToolkit.toString(TAR), command.getBytes(), currentResponse.getData());
        }
    }

    private void tryBeingSmart() throws CardException {

        System.out.println("Trying to be smart. Scanning " + _smart_count + " random TARs to determine a false response other than standard.");

        List<ByteArray> random_responses = new ArrayList();

        for (int i = 0; i < _smart_count; i++) {
            int int_TAR = (int) (Math.random() * 0xFFFFFF); // generate random TAR
            byte[] currentTAR = new byte[]{(byte) (int_TAR >>> 16), (byte) (int_TAR >>> 8), (byte) (int_TAR)};
            CommandPacket cp = new CommandPacket();
            ResponseAPDU response = testTAR(currentTAR, cp);
            ResponseAPDU handled_response = Helpers.handleSIMResponse(response);

            ResponsePacket rp = new ResponsePacket();
            try {
                rp.parse(handled_response.getData(), true);
                random_responses.add(new ByteArray(new byte[]{rp.getStatusCode()})); // if we managed to parse it as ResponsePacket only add its status code
                System.out.println("Generated TAR " + HexToolkit.toString(currentTAR) + " returned " + HexToolkit.toString(handled_response.getBytes()) + "; added PoR status code: " + String.format("%02X", rp.getStatusCode()));
                continue;
            } catch (ParseException e) {
            }
            random_responses.add(new ByteArray(handled_response.getBytes()));
            System.out.println("Generated TAR " + HexToolkit.toString(currentTAR) + " returned " + HexToolkit.toString(handled_response.getBytes()));
        }

        Set<ByteArray> uniqueResponses = new HashSet(random_responses);
        Map<ByteArray, Integer> counts = new HashMap();

        for (ByteArray one : uniqueResponses) {
            int count = Collections.frequency(random_responses, one);
            counts.put(one, count);
        }

        counts = Helpers.sortByValuesDesc(counts);
        ByteArray most_common = counts.entrySet().iterator().next().getKey();

        System.out.println("Response " + most_common.getStringData() + " determined as most common - therefore considered a false response.");

        skip_responses.add(most_common);
    }

    private ArrayList<ByteArray> prepareTARlist() {
        ArrayList<ByteArray> TARlist = new ArrayList<>();

        /* list of all printable punctation characters and numbers */
        ArrayList<Character> punctEtcChars = new ArrayList<>();
        for (Integer oneChar : Range.range(0x21, 0x40)) { // this is ! up to @
            punctEtcChars.add((char) oneChar.intValue());
        }
        for (Integer oneChar : Range.range(0x5B, 0x60)) { // this is [ up to `
            punctEtcChars.add((char) oneChar.intValue());
        }
        for (Integer oneChar : Range.range(0x7B, 0x7E)) { // this is { up to ~
            punctEtcChars.add((char) oneChar.intValue());
        }

        /* list of all printable lowercase characters */
        ArrayList<Character> lowerCaseChars = new ArrayList<>();
        for (Integer oneChar : Range.range(0x61, 0x7A)) { // this is a up to z
            lowerCaseChars.add((char) oneChar.intValue());
        }

        /* list of all printable uppercase characters */
        ArrayList<Character> upperCaseChars = new ArrayList<>();
        for (Integer oneChar : Range.range(0x41, 0x5A)) { // this is A up to Z
            upperCaseChars.add((char) oneChar.intValue());
        }

        /* lowercase chars + various puncts together */
        ArrayList<Character> lowerCaseAndPunct = new ArrayList<>();
        lowerCaseAndPunct.addAll(lowerCaseChars);
        lowerCaseAndPunct.addAll(punctEtcChars);

        /* uppercase chars + various puncts together */
        ArrayList<Character> upperCaseAndPunct = new ArrayList<>();
        upperCaseAndPunct.addAll(upperCaseChars);
        upperCaseAndPunct.addAll(punctEtcChars);

        ICombinatoricsVector<Character> lowerVector = Factory.createVector(lowerCaseAndPunct);
        Generator<Character> lowergen = Factory.createPermutationWithRepetitionGenerator(lowerVector, 3);

        ICombinatoricsVector<Character> upperVector = Factory.createVector(upperCaseAndPunct);
        Generator<Character> uppergen = Factory.createPermutationWithRepetitionGenerator(upperVector, 3);

        for (ICombinatoricsVector<Character> lowerperm : lowergen) {
            TARlist.add(new ByteArray(CharacterListToByteArray(lowerperm.getVector())));
        }

        for (ICombinatoricsVector<Character> upperperm : uppergen) {
            TARlist.add(new ByteArray(CharacterListToByteArray(upperperm.getVector())));
        }

        for (Integer int_TAR : Range.range(0x000000, 0x0000FF)) {
            TARlist = addToTARlist(int_TAR, TARlist);
        }

        for (Integer int_TAR : Range.range(0x000100, 0x00010F)) {
            TARlist = addToTARlist(int_TAR, TARlist);
        }

        for (Integer int_TAR : Range.range(0x000200, 0x00020F)) {
            TARlist = addToTARlist(int_TAR, TARlist);
        }

        for (Integer int_TAR : Range.range(0x000300, 0x00030F)) {
            TARlist = addToTARlist(int_TAR, TARlist);
        }

        for (Integer int_TAR : Range.range(0x000400, 0x00040F)) {
            TARlist = addToTARlist(int_TAR, TARlist);
        }

        for (Integer int_TAR : Range.range(0x000500, 0x00050F)) {
            TARlist = addToTARlist(int_TAR, TARlist);
        }

        for (Integer int_TAR : Range.range(0x3F0000, 0x3F003F)) {
            TARlist = addToTARlist(int_TAR, TARlist);
        }

        for (Integer int_TAR : Range.range(0x800000, 0x8000FF)) {
            TARlist = addToTARlist(int_TAR, TARlist);
        }

        for (Integer int_TAR : Range.range(0xA00000, 0xA000FF)) {
            TARlist = addToTARlist(int_TAR, TARlist);
        }

        for (Integer int_TAR : Range.range(0xB00000, 0xB000FF)) {
            TARlist = addToTARlist(int_TAR, TARlist);
        }

        for (Integer int_TAR : Range.range(0xBFFF00, 0xBFFFFF)) {
            TARlist = addToTARlist(int_TAR, TARlist);
        }

        for (Integer int_TAR : Range.range(0xC00000, 0xC000FF)) {
            TARlist = addToTARlist(int_TAR, TARlist);
        }

        for (Integer int_TAR : Range.range(0xD00000, 0xD000FF)) {
            TARlist = addToTARlist(int_TAR, TARlist);
        }

        for (Integer int_TAR : Range.range(0xEED000, 0xEEEFFF)) {
            TARlist = addToTARlist(int_TAR, TARlist);
        }

        for (Integer int_TAR : Range.range(0xFFFF00, 0xFFFFFF)) {
            TARlist = addToTARlist(int_TAR, TARlist);
        }

        // as punctation letters gets repeated in the two permutations (lowercase + punct) and (uppercase + punct) we need to deduplicate those TARs containing punct chars only
        Set<ByteArray> fakeHashSet = new TreeSet<>(new Comparator<ByteArray>() {
            @Override
            public int compare(ByteArray ba1, ByteArray ba2) {
                return HexToolkit.compareTARs(ba1.getData(), ba2.getData());
            }
        });
        fakeHashSet.addAll(TARlist);
        TARlist.clear();
        TARlist.addAll(fakeHashSet);

        Collections.sort(TARlist, new Comparator<ByteArray>() {
            @Override
            public int compare(ByteArray ba1, ByteArray ba2) {
                return HexToolkit.compareTARs(ba1.getData(), ba2.getData());
            }
        });

        return TARlist;
    }

    private ArrayList<ByteArray> addToTARlist(Integer int_TAR, ArrayList<ByteArray> TARlist) {
        TARlist.add(new ByteArray(new byte[]{(byte) (int_TAR.intValue() >>> 16), (byte) (int_TAR.intValue() >>> 8), (byte) (int_TAR.intValue())}));
        return TARlist;
    }

    private byte[] CharacterListToByteArray(List<Character> charList) {
        byte[] result = new byte[charList.size()];
        for (int i = 0; i < charList.size(); i++) {
            result[i] = (byte) charList.get(i).charValue();
        }
        return result;
    }
}
