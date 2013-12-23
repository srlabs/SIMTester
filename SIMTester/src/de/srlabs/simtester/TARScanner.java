package de.srlabs.simtester;

import de.srlabs.simlib.APDUToolkit;
import de.srlabs.simlib.AutoTerminalProfile;
import de.srlabs.simlib.CommandPacket;
import de.srlabs.simlib.Debug;
import de.srlabs.simlib.HexToolkit;
import de.srlabs.simlib.LoggingUtils;
import de.srlabs.simlib.OTASMS;
import de.srlabs.simlib.Range;
import de.srlabs.simlib.ResponsePacket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.smartcardio.CardException;
import javax.smartcardio.ResponseAPDU;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import org.paukov.combinatorics.Factory;
import org.paukov.combinatorics.Generator;
import org.paukov.combinatorics.ICombinatoricsVector;

public class TARScanner {

    private final static boolean LOCAL_DEBUG = false;
    private final static boolean DEBUG = Debug.DEBUG || LOCAL_DEBUG;
    private final static int HIGHEST_TAR = 16777215; // this is 0xFFFFFF which is the highest possible TAR value
    private static int _keyset;

    private static void initScan(int amountOfTARs) throws CardException {

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
        System.out.println("Starting TAR scanning, going to go over " + amountOfTARs + " TARs, go get a (few) coffee(s) ..");
        System.out.println();
    }

    public static void scanAllTARs(String startingTAR, int keyset, CSVWriter writer) throws Exception {
        if (!startingTAR.matches("[0-9A-F]+") || startingTAR.length() != 6) {
            System.err.println(LoggingUtils.formatDebugMessage("TAR value has to be hexadecimal value, 3 bytes long, yours is NOT! value = " + startingTAR));
            return;
        }

        _keyset = keyset;
        int startingTAR_int = Integer.valueOf(startingTAR, 16);
        int loopUntil = HIGHEST_TAR - startingTAR_int;

        initScan(loopUntil);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i <= loopUntil; i++) {
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

            testTAR(currentTAR, writer);

        }
    }

    public static void scanRangesOfTARs(int keyset, CSVWriter writer) throws Exception {
        _keyset = keyset;
        ArrayList<byte[]> TARList = prepareTARlist();
        int loopUntil = TARList.size();

        initScan(loopUntil);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < loopUntil; i++) {
            byte[] currentTAR = TARList.get(i);
            if (i == (loopUntil - 1) || (i % 100) == 0) {
                long currentTime = System.currentTimeMillis();
                if (i != 0) {
                    long timePassed = currentTime - startTime;
                    long diffTime = (long) (((double) timePassed / (double) i) * (loopUntil - i));
                    Duration duration = DatatypeFactory.newInstance().newDuration(diffTime);
                    System.out.printf("Processing TAR: %s, already processed: %d TARs, to process: %d TARs, approximate remaining time: %d days, %d hours, %d minutes, %d seconds\n", HexToolkit.toString(currentTAR), i, (loopUntil - i), duration.getDays(), duration.getHours(), duration.getMinutes(), duration.getSeconds());
                }
            }

            testTAR(currentTAR, writer);

        }

    }

    private static void testTAR(byte[] TAR, CSVWriter writer) throws CardException {
        CommandPacket cp = new CommandPacket();

        cp.setKeyset(_keyset);
        cp.setCounterManegement(CommandPacket.CNTR_NO_CNTR_AVAILABLE);
        cp.setCounter(1);
        cp.setPoR(true); // request PoR
        cp.setPoRSecurity(CommandPacket.POR_SECURITY_CC); // sign the PoR packet
        cp.setPoRMode(CommandPacket.POR_MODE_SMS_DELIVER_REPORT);
        cp.setUserData(HexToolkit.fromString("A0A40000023F00"));

        OTASMS otasms = new OTASMS();

        cp.setTAR(TAR);
        otasms.setCommandPacket(cp);
        ResponseAPDU response = otasms.send();

        if (DEBUG) {
            System.out.println(LoggingUtils.formatDebugMessage("ResponseAPDU bytes: " + HexToolkit.toString(response.getBytes())));
        }

        response.getBytes();

        if ((byte) response.getSW1() == (byte) 0x9E || (byte) response.getSW1() == (byte) 0x9F) {
            byte[] response_data = APDUToolkit.getResponse(response.getSW2()).getData();
            ResponsePacket rp = new ResponsePacket();
            rp.parse(response_data);
            byte status_code = rp.getStatusCode();

            if (status_code != 9) {
                System.out.println("GOT VALID TAR!! (" + HexToolkit.toString(TAR) + "), PoR status code: " + HexToolkit.toString(status_code));
                writer.writeLine(HexToolkit.toString(TAR), cp.getBytes(), response_data);
            }
        } else if ((byte) response.getSW1() == (byte) 0x91) { // fetch
            ResponseAPDU r = APDUToolkit.performFetch(response.getSW2());
            byte[] fetched_data = r.getData();
            System.out.println("SW = " + Integer.toHexString(r.getSW()) + ", card responded with FETCH, fetched_data = " + HexToolkit.toString(fetched_data));
            writer.writeLine(HexToolkit.toString(TAR), cp.getBytes(), fetched_data);
            if (r.getData().length > 0 && (byte) 0xD0 == r.getData()[0]) {
                System.out.println("Sending Terinal Response to the Proactive Command..");
                r = AutoTerminalProfile.handleProactiveCommand(r); // in case applet requests a proactive command (send sms, display text, whatever), we need to send Terminal Response back saying we did it, otherwise it won't work (eg. re-execution without card reset won't work properly as there is still pending proactive command)
                System.out.println("SW after Terminal Response: " + Integer.toHexString(r.getSW()));
            }

        } else {
            System.out.println("RESPONSE OTHER THAN ERROR!! (" + HexToolkit.toString(TAR) + "), response_data: " + HexToolkit.toString(response.getBytes()));
            writer.writeLine(HexToolkit.toString(TAR), cp.getBytes(), response.getBytes());
        }
    }

    private static ArrayList<byte[]> prepareTARlist() {
        ArrayList<byte[]> TARlist = new ArrayList<>();

        /* list of all printable punctation characters and numbers */
        ArrayList<Character> punctEtcChars = new ArrayList<>();
        for (Integer oneChar : Range.range(33, 64)) {
            punctEtcChars.add((char) oneChar.intValue());
        }
        for (Integer oneChar : Range.range(91, 96)) {
            punctEtcChars.add((char) oneChar.intValue());
        }
        for (Integer oneChar : Range.range(123, 126)) {
            punctEtcChars.add((char) oneChar.intValue());
        }

        /* list of all printable lowercase characters */
        ArrayList<Character> lowerCaseChars = new ArrayList<>();
        for (Integer oneChar : Range.range(97, 122)) {
            lowerCaseChars.add((char) oneChar.intValue());
        }

        /* list of all printable uppercase characters */
        ArrayList<Character> upperCaseChars = new ArrayList<>();
        for (Integer oneChar : Range.range(97, 122)) {
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
            TARlist.add(CharacterListToByteArray(lowerperm.getVector()));
        }

        for (ICombinatoricsVector<Character> upperperm : uppergen) {
            TARlist.add(CharacterListToByteArray(upperperm.getVector()));
        }

        for (Integer int_TAR : Range.range(0x000000, 0x0000FF)) {
            TARlist.add(new byte[]{(byte) (int_TAR.intValue() >>> 16), (byte) (int_TAR.intValue() >>> 8), (byte) (int_TAR.intValue())});
        }

        for (Integer int_TAR : Range.range(0x000100, 0x00010F)) {
            TARlist.add(new byte[]{(byte) (int_TAR.intValue() >>> 16), (byte) (int_TAR.intValue() >>> 8), (byte) (int_TAR.intValue())});
        }

        for (Integer int_TAR : Range.range(0x000200, 0x00020F)) {
            TARlist.add(new byte[]{(byte) (int_TAR.intValue() >>> 16), (byte) (int_TAR.intValue() >>> 8), (byte) (int_TAR.intValue())});
        }

        for (Integer int_TAR : Range.range(0x000300, 0x00030F)) {
            TARlist.add(new byte[]{(byte) (int_TAR.intValue() >>> 16), (byte) (int_TAR.intValue() >>> 8), (byte) (int_TAR.intValue())});
        }

        for (Integer int_TAR : Range.range(0x000400, 0x00040F)) {
            TARlist.add(new byte[]{(byte) (int_TAR.intValue() >>> 16), (byte) (int_TAR.intValue() >>> 8), (byte) (int_TAR.intValue())});
        }

        for (Integer int_TAR : Range.range(0x000500, 0x00050F)) {
            TARlist.add(new byte[]{(byte) (int_TAR.intValue() >>> 16), (byte) (int_TAR.intValue() >>> 8), (byte) (int_TAR.intValue())});
        }

        for (Integer int_TAR : Range.range(0x3F0000, 0x3F003F)) {
            TARlist.add(new byte[]{(byte) (int_TAR.intValue() >>> 16), (byte) (int_TAR.intValue() >>> 8), (byte) (int_TAR.intValue())});
        }

        for (Integer int_TAR : Range.range(0x800000, 0x8000FF)) {
            TARlist.add(new byte[]{(byte) (int_TAR.intValue() >>> 16), (byte) (int_TAR.intValue() >>> 8), (byte) (int_TAR.intValue())});
        }

        for (Integer int_TAR : Range.range(0xA00000, 0xA000FF)) {
            TARlist.add(new byte[]{(byte) (int_TAR.intValue() >>> 16), (byte) (int_TAR.intValue() >>> 8), (byte) (int_TAR.intValue())});
        }

        for (Integer int_TAR : Range.range(0xB00000, 0xB000FF)) {
            TARlist.add(new byte[]{(byte) (int_TAR.intValue() >>> 16), (byte) (int_TAR.intValue() >>> 8), (byte) (int_TAR.intValue())});
        }

        for (Integer int_TAR : Range.range(0xBFFF00, 0xBFFFFF)) {
            TARlist.add(new byte[]{(byte) (int_TAR.intValue() >>> 16), (byte) (int_TAR.intValue() >>> 8), (byte) (int_TAR.intValue())});
        }

        for (Integer int_TAR : Range.range(0xC00000, 0xC000FF)) {
            TARlist.add(new byte[]{(byte) (int_TAR.intValue() >>> 16), (byte) (int_TAR.intValue() >>> 8), (byte) (int_TAR.intValue())});
        }

        for (Integer int_TAR : Range.range(0xD00000, 0xD000FF)) {
            TARlist.add(new byte[]{(byte) (int_TAR.intValue() >>> 16), (byte) (int_TAR.intValue() >>> 8), (byte) (int_TAR.intValue())});
        }

        for (Integer int_TAR : Range.range(0xEED000, 0xEEEFFF)) {
            TARlist.add(new byte[]{(byte) (int_TAR.intValue() >>> 16), (byte) (int_TAR.intValue() >>> 8), (byte) (int_TAR.intValue())});
        }

        for (Integer int_TAR : Range.range(0xFFFF00, 0xFFFFFF)) {
            TARlist.add(new byte[]{(byte) (int_TAR.intValue() >>> 16), (byte) (int_TAR.intValue() >>> 8), (byte) (int_TAR.intValue())});
        }

        Collections.sort(TARlist, new Comparator<byte[]>() {
            @Override
            public int compare(byte[] ba1, byte[] ba2) {
                return HexToolkit.compareTARs(ba1, ba2);
            }
        });

        return TARlist;
    }

    private static byte[] CharacterListToByteArray(List<Character> charList) {
        byte[] result = new byte[charList.size()];
        for (int i = 0; i < charList.size(); i++) {
            result[i] = (byte) charList.get(i).charValue();
        }
        return result;
    }
}
