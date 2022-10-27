package de.srlabs.simtester;

import de.srlabs.simlib.CommandPacket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FuzzerFactory {

    private final static Map<Integer, FuzzerData> fuzzers = new HashMap<Integer, FuzzerData>() {
        {
            //FuzzerData(String name, byte counter, int kic, int kid, boolean cipherCP, boolean requestPoR, boolean cipherPoR)

            // special case for WIB/SAT
            put(0, new FuzzerData("fuzzer0", CommandPacket.CNTR_NO_CNTR_AVAILABLE,
                    CommandPacket.KIC_ALGO_IMPLICIT, CommandPacket.KID_ALGO_IMPLICIT, false, false));

            // just signature
            put(1, new FuzzerData("fuzzer1", CommandPacket.CNTR_NO_CNTR_AVAILABLE,
                    CommandPacket.KIC_ALGO_IMPLICIT, CommandPacket.KID_ALGO_IMPLICIT, true, false));

            put(2, new FuzzerData("fuzzer2", CommandPacket.CNTR_CNTR_AVAILABLE,
                    CommandPacket.KIC_ALGO_IMPLICIT, CommandPacket.KID_ALGO_IMPLICIT, true, false));

            put(3, new FuzzerData("fuzzer3", CommandPacket.CNTR_CNTR_HIGHER,
                    CommandPacket.KIC_ALGO_IMPLICIT, CommandPacket.KID_ALGO_IMPLICIT, true, false));

            put(4, new FuzzerData("fuzzer4", CommandPacket.CNTR_CNTR_ONE_HIGHER,
                    CommandPacket.KIC_ALGO_IMPLICIT, CommandPacket.KID_ALGO_IMPLICIT, true, false));

            put(5, new FuzzerData("fuzzer5", CommandPacket.CNTR_CNTR_ONE_HIGHER,
                    CommandPacket.KIC_ALGO_IMPLICIT, CommandPacket.KID_ALGO_DES_CBC, true, false));

            put(6, new FuzzerData("fuzzer6", CommandPacket.CNTR_CNTR_ONE_HIGHER,
                    CommandPacket.KIC_ALGO_IMPLICIT, CommandPacket.KID_ALGO_3DES_CBC_2KEYS, true, false));

            put(7, new FuzzerData("fuzzer7", CommandPacket.CNTR_CNTR_ONE_HIGHER,
                    CommandPacket.KIC_ALGO_IMPLICIT, CommandPacket.KID_ALGO_3DES_CBC_3KEYS, true, false));

            put(8, new FuzzerData("fuzzer8", CommandPacket.CNTR_CNTR_ONE_HIGHER,
                    CommandPacket.KIC_ALGO_3DES_CBC_3KEYS, CommandPacket.KID_ALGO_3DES_CBC_3KEYS, true, false));

            // ciphered PoRs - same old fuzzers as we used (above) with ciphering enabled for PoRs
            put(9, new FuzzerData("fuzzer9", CommandPacket.CNTR_NO_CNTR_AVAILABLE,
                    CommandPacket.KIC_ALGO_IMPLICIT, CommandPacket.KID_ALGO_IMPLICIT, true, true));

            put(10, new FuzzerData("fuzzer10", CommandPacket.CNTR_CNTR_AVAILABLE,
                    CommandPacket.KIC_ALGO_IMPLICIT, CommandPacket.KID_ALGO_IMPLICIT, true, true));

            put(11, new FuzzerData("fuzzer11", CommandPacket.CNTR_CNTR_HIGHER,
                    CommandPacket.KIC_ALGO_IMPLICIT, CommandPacket.KID_ALGO_IMPLICIT, true, true));

            put(12, new FuzzerData("fuzzer12", CommandPacket.CNTR_CNTR_ONE_HIGHER,
                    CommandPacket.KIC_ALGO_IMPLICIT, CommandPacket.KID_ALGO_IMPLICIT, true, true));

            put(13, new FuzzerData("fuzzer13", CommandPacket.CNTR_CNTR_ONE_HIGHER,
                    CommandPacket.KIC_ALGO_DES_CBC, CommandPacket.KID_ALGO_IMPLICIT, true, true));

            put(14, new FuzzerData("fuzzer14", CommandPacket.CNTR_CNTR_ONE_HIGHER,
                    CommandPacket.KIC_ALGO_3DES_CBC_2KEYS, CommandPacket.KID_ALGO_IMPLICIT, true, true));

            put(15, new FuzzerData("fuzzer15", CommandPacket.CNTR_CNTR_ONE_HIGHER,
                    CommandPacket.KIC_ALGO_3DES_CBC_3KEYS, CommandPacket.KID_ALGO_IMPLICIT, true, true));

            put(16, new FuzzerData("fuzzer16", CommandPacket.CNTR_CNTR_ONE_HIGHER,
                    CommandPacket.KIC_ALGO_3DES_CBC_3KEYS, CommandPacket.KID_ALGO_3DES_CBC_3KEYS, true, true));
        }
    };

    public static FuzzerData getFuzzer(Integer fuzzerNr) {
        return fuzzers.get(fuzzerNr);
    }

    public static ArrayList<FuzzerData> getAllFuzzers() {
        return new ArrayList<FuzzerData>(fuzzers.values());
    }

    public static int getAmountOfFuzzers() {
        return fuzzers.size();
    }
    public static List<String> defaultTARs = new ArrayList<String>() {
        {
            add("RAM:000000");
            add("WIB:000001");
            add("WIB:000002");
            add("WIB:000003");
            add("WIB:000004");
            add("WIB:000005");
            add("WIB:000006");
            add("WIB:000007");
            add("WIB:000008");
            add("WIB:000009");
            add("RFM:00000A");
            add("RFM:00000B");
            add("RFM:00000C");
            add("RFM:00000D");
            add("RFM:00004F");
            add("RFM:000057");
            add("RFM:000070");
            add("RFM:000076");
            add("RFM:000080");
            add("RFM:000092");
            add("RFM:0000B6");
            add("RFM:0000E2");
            add("RFM:000203");
            add("RFM:000304");
            add("RFM:000503");
            add("RFM:010001");
            add("RFM:010101");
            add("RFM:010203");
            add("RFM:012345");
            add("RFM:012347");
            add("RFM:060504");
            add("RFM:100000");
            add("RFM:111212");
            add("RFM:212223");
            add("RFM:260500");
            add("RFM:313131");
            add("RFM:385300");
            add("RFM:3F0000");
            add("RFM:3F0001");
            add("RFM:3F0002");
            add("RFM:3F0010");
            add("RFM:3F0011");
            add("RFM:41444E");
            add("RFM:414C4F");
            add("RFM:415256");
            add("RFM:415345");
            add("RFM:424950");
            add("RFM:425058");
            add("RFM:434354");
            add("RFM:443231");
            add("RFM:474341");
            add("RFM:47534D");
            add("RFM:484353");
            add("RFM:49434D");
            add("RFM:494D45");
            add("RFM:4C5041");
            add("RFM:4D4552");
            add("RFM:4D4C4D");
            add("RFM:4E4147");
            add("RFM:4E5550");
            add("RFM:4E5553");
            add("RFM:4E5650");
            add("RFM:4F4350");
            add("RFM:504F53");
            add("SAT:505348");
            add("RFM:514F43");
            add("RFM:524144");
            add("RFM:524648");
            add("RFM:524A49");
            add("RFM:54454C");
            add("RFM:524F4D");
            add("RFM:533347");
            add("SAT:534054");
            add("RFM:534143");
            add("RFM:534441");
            add("RFM:534F44");
            add("RFM:53534D");
            add("RFM:535353");
            add("RFM:564153");
            add("RFM:64646D");
            add("RFM:800001");
            add("RFM:800002");
            add("RFM:800040");
            add("RFM:800041");
            add("RFM:B00000");
            add("RFM:B00001");
            add("RFM:B00002");
            add("RFM:B00003");
            add("RFM:B0000F");
            add("RFM:B00010");
            add("RFM:B00011");
            add("RFM:B00012");
            add("RFM:B00013");
            add("RFM:B00020");
            add("RFM:B00021");
            add("RFM:B00030");
            add("RFM:B00040");
            add("RFM:B00041");
            add("RFM:B00042");
            add("RFM:B00050");
            add("RFM:B000F1");
            add("RFM:B00120");
            add("RFM:B00140");
            add("RFM:B00141");
            add("RFM:B00142");
            add("RFM:B00143");
            add("RFM:B00144");
            add("RFM:B00145");
            add("RFM:B11000");
            add("RFM:B20100");
            add("RFM:B20102");
            add("RFM:BAFE02");
            add("WIB:BFFF00");
            add("WIB:BFFF01");
            add("WIB:BFFF02");
            add("WIB:BFFF03");
            add("WIB:BFFF04");
            add("WIB:BFFF05");
            add("WIB:BFFF15");
            add("WIB:BFFF22");
            add("WIB:BFFFBA");
            add("WIB:BFFFEE");
            add("WIB:BFFFFF");
            add("RFM:C00000");
            add("RFM:C0013D");
            add("RFM:C001AA");
            add("RFM:C001AB");
            add("RFM:C001AD");
            add("RFM:D00003");
            add("RFM:EED200");
            add("RFM:EED201");
            add("RFM:EEE200");
            add("RFM:EEE201");
            add("RFM:FFFF01");
            add("RFM:FFFFFF");
        }
    };
}
