package de.srlabs.simlib;

import java.text.ParseException;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.smartcardio.CardException;
import javax.smartcardio.ResponseAPDU;

public class Helpers {

    private final static boolean LOCAL_DEBUG = false;
    private final static boolean DEBUG = Debug.DEBUG || LOCAL_DEBUG;

    public static ResponseAPDU handleSIMResponse(ResponseAPDU response) throws CardException {
        return handleSIMResponse(response, true);
    }

    public static ResponseAPDU handleSIMResponse(ResponseAPDU response, boolean printSummary) throws CardException {
        if (DEBUG) {
            System.out.println(LoggingUtils.formatDebugMessage("Handling SIM response: " + HexToolkit.toString(response.getBytes())));
        }

        if ((byte) response.getSW1() == (byte) 0x9E || (byte) response.getSW1() == (byte) 0x9F
                || (SIMLibrary.third_gen_apdu && ((byte) response.getSW1() == (byte) 0x62 || (byte) response.getSW1() == (byte) 0x61))) {
            return APDUToolkit.getResponse(response.getSW2());
        } else if ((byte) response.getSW1() == (byte) 0x91) {
            ResponseAPDU fetch_response = APDUToolkit.performFetch(response.getSW2());
            byte[] fetched_data = fetch_response.getData();

            if (fetched_data[0] == (byte) 0xD0) { // handling of proactive data..
                ProactiveCommand pc;

                try {
                    pc = new ProactiveCommand(fetched_data);
                } catch (ParseException e) {
                    System.out.println("\033[95m" + "WARNING! Unable to parse ProactiveCommand, data = " + HexToolkit.toString(fetched_data) + "\033[0m");
                    return fetch_response;
                }

                if (printSummary) {
                    String summary = pc.getSummary();

                    if (!"".equals(summary)) {
                        System.out.println("\033[90m" + "Proactive command (" + "\033[95m" + pc.getType() + "\033[90m" + ") identified, details: " + "\033[95m" + summary + "\033[90m" + "; trying to handle it.." + "\033[0m");
                    } else {
                        System.out.println("\033[90m" + "Proactive command (" + "\033[95m" + pc.getType() + "\033[90m" + ") identified, trying to handle it.." + "\033[0m");
                    }
                }

                ResponseAPDU proactive_response = AutoTerminalProfile.handleProactiveCommand(fetch_response);

                if (proactive_response.getSW() != 0x9000) {
                    System.out.println("\033[95m" + "WARNING! Response (SW) to terminal response apdu is not 0x9000: " + HexToolkit.toString(proactive_response.getBytes()) + "\033[0m");
                }
            }
            return fetch_response;

        } else {
            return response;
        }

    }

    public static <K, V extends Comparable> Map<K, V> sortByValuesDesc(Map<K, V> map) {
        List<Map.Entry<K, V>> entries = new LinkedList(map.entrySet());

        Collections.sort(entries, new Comparator<Map.Entry<K, V>>() {

            @Override
            public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
                return -(o1.getValue().compareTo(o2.getValue()));
            }
        });

        //LinkedHashMap will keep the keys in the order they are inserted
        //which is currently sorted on natural ordering
        Map<K, V> sortedMap = new LinkedHashMap();

        for (Map.Entry<K, V> entry : entries) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;
    }

    /**
     * Compares two version strings.
     *
     * Use this instead of String.compareTo() for a non-lexicographical
     * comparison that works for version strings. e.g. "1.10".compareTo("1.6").
     *
     * @note It does not work if "1.10" is supposed to be equal to "1.10.0".
     *
     * @param str1 a string of ordinal numbers separated by decimal points.
     * @param str2 a string of ordinal numbers separated by decimal points.
     * @return The result is a negative integer if str1 is _numerically_ less
     * than str2. The result is a positive integer if str1 is _numerically_
     * greater than str2. The result is zero if the strings are _numerically_
     * equal.
     */
    public static Integer versionCompare(String str1, String str2) {
        String[] vals1 = str1.split("\\.");
        String[] vals2 = str2.split("\\.");
        int i = 0;
        // set index to first non-equal ordinal or length of shortest version string
        while (i < vals1.length && i < vals2.length && vals1[i].equals(vals2[i])) {
            i++;
        }
        // compare first non-equal ordinal number
        if (i < vals1.length && i < vals2.length) {
            int diff = Integer.valueOf(vals1[i]).compareTo(Integer.valueOf(vals2[i]));
            return Integer.signum(diff);
        } // the strings are equal or one string is a substring of the other
        // e.g. "1.2.3" = "1.2.3" or "1.2.3" < "1.2.3.4"
        else {
            return Integer.signum(vals1.length - vals2.length);
        }
    }
}
