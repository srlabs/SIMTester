package de.srlabs.simlib;

import java.util.HashMap;
import java.util.Map;

public class ISIMCardFileMapping extends SimCardFileMapping {

    @Override
    protected Map<String, String[]> getFilesDetails() {
        return new HashMap<String, String[]>() {{
            put("7fff/6f02", new String[]{"IMPI", "IMS private user identity"});
            put("7fff/6f03", new String[]{"DOMAIN", "Home Network Domain Name"});
            put("7fff/6f04", new String[]{"IMPU", "IMS public user identity"});
            put("7fff/6f06", new String[]{"ARR", "Access Rule Reference"});
            put("7fff/6f07", new String[]{"IST", "ISIM Service Table"});
            put("7fff/6f09", new String[]{"P-CSCF", "P-CSCF Address"});
            put("7fff/6f3c", new String[]{"SMS", "Short messages"});
            put("7fff/6f42", new String[]{"SMSP", "Short message service parameters"});
            put("7fff/6f43", new String[]{"SMSS", "SMS status"});
            put("7fff/6f47", new String[]{"SMSR", "Short message status reports"});
            put("7fff/6fad", new String[]{"AD", "Administrative Data"});
            put("7fff/6fd5", new String[]{"GBABP", "GBA Bootstrapping parameters"});
            put("7fff/6fd7", new String[]{"GBANL", "GBA NAF List"});
            put("7fff/6fdd", new String[]{"NAFKCA", "NAF Key Centre Address"});
            put("7fff/6fe7", new String[]{"UICCIARI", "UICC IARI"});
        }};
    }
}
