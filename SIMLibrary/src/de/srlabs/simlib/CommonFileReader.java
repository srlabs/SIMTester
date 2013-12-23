package de.srlabs.simlib;

import java.io.FileNotFoundException;
import javax.smartcardio.CardException;

public class CommonFileReader {

    private final static boolean LOCAL_DEBUG = false;
    private final static boolean DEBUG = Debug.DEBUG || LOCAL_DEBUG;

    /* ICCID - 3f002fe2 (transparent file)
     * IMSI - 3f007f206f07 (transparent file) (ex. content 082903301054360547)
     * MSISDN - 3f007f106f40 (linear file)
     * EF_DIR (if it's present) 3f002f00 (linear file)
     * EF_MANUAREA (if it's present)
     * EF_ADN 3f007f106f3a
     * EF_SST 3f007f206f38
     */

    public static byte[] readSMSP() {

        if (DEBUG) {
            System.out.println(LoggingUtils.formatDebugMessage("reading EF_SMSP file"));
        }

        SimCardFile file;

        try {
            file = FileManagement.selectPath("3f007f206f42");
        } catch (FileNotFoundException | CardException e) {
            file = null;
        }
        
        System.out.println(file.getFileId());

        if (null != file) { // in case there's a problem reading a file we don't wanna throw exception but rather continue with other files/actions

            byte[] content;
                    
            try {
                content = ((SimCardTransparentFile) file).getContent();
            } catch (CardException e) {
                content = null;
            }

            if (null != content) {
                return content;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public static byte[] readRawMSISDN() throws CardException {
        if (DEBUG) {
            System.out.println(LoggingUtils.formatDebugMessage("reading EF_MSISDN file"));
        }

        SimCardFile file;

        try {
            file = FileManagement.selectPath("3f007f106f40");
        } catch (FileNotFoundException e) {
            file = null;
        }

        if (null != file) { // in case there's a problem reading a file we don't wanna throw exception but rather continue with other files/actions
            byte[] content = ((SimCardLinearFixedFile) file).getFirstRecord();
            if (null != content) {
                if (content[content.length - 14] != (byte) 0xff) {
                    return content;
                } else {
                    return null;
                }
            } else {
                return null;
            }
        } else {
            return null;
        }

    }

    public static String decodeMSISDN(byte[] msisdn_content) {
        int alpha_id_size = msisdn_content.length - 14; // 14 bytes is the fixed size for the rest of the record
        byte len = msisdn_content[alpha_id_size];
        byte ton_npi = msisdn_content[alpha_id_size + 1];
        String result = "";
        
        if (HexToolkit.isBitSet(ton_npi, 4) && !HexToolkit.isBitSet(ton_npi, 5) && !HexToolkit.isBitSet(ton_npi, 6)) {
            result += "+";
        }
        
        for (int i=alpha_id_size+2; i<=(len+alpha_id_size); i++) {
            result += HexToolkit.toString(HexToolkit.swap(msisdn_content[i]));
        }
        
        return result;
    }

    
    
    public static byte[] readSST() {

        if (DEBUG) {
            System.out.println(LoggingUtils.formatDebugMessage("reading EF_SST file"));
        }

        SimCardFile file;

        try {
            file = FileManagement.selectPath("3f007f206f38");
        } catch (FileNotFoundException | CardException e) {
            file = null;
        }

        if (null != file) { // in case there's a problem reading a file we don't wanna throw exception but rather continue with other files/actions

            byte[] content;
                    
            try {
                content = ((SimCardTransparentFile) file).getContent();
            } catch (CardException e) {
                content = null;
            }

            if (null != content) {
                return content;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public static void decode_GSM_EF_SST(byte[] SSTContent) {

        if (DEBUG) {
            System.out.println(LoggingUtils.formatDebugMessage("length of SSTContent (" + HexToolkit.toString(SSTContent) + ") = " + SSTContent.length));
        }

        for (int i = 0; i < SSTContent.length; i++) {
            System.out.println("Byte: " + i + " (" + HexToolkit.toString(SSTContent[i]) + "), Service " + (1 + i * 4) + ": allocated: " + (HexToolkit.isBitSet(SSTContent[i], 0) ? '1' : '0') + ", activated: " + (HexToolkit.isBitSet(SSTContent[i], 1) ? '1' : '0'));
            System.out.println("Byte: " + i + " (" + HexToolkit.toString(SSTContent[i]) + "), Service " + (2 + i * 4) + ": allocated: " + (HexToolkit.isBitSet(SSTContent[i], 2) ? '1' : '0') + ", activated: " + (HexToolkit.isBitSet(SSTContent[i], 3) ? '1' : '0'));
            System.out.println("Byte: " + i + " (" + HexToolkit.toString(SSTContent[i]) + "), Service " + (3 + i * 4) + ": allocated: " + (HexToolkit.isBitSet(SSTContent[i], 4) ? '1' : '0') + ", activated: " + (HexToolkit.isBitSet(SSTContent[i], 5) ? '1' : '0'));
            System.out.println("Byte: " + i + " (" + HexToolkit.toString(SSTContent[i]) + "), Service " + (4 + i * 4) + ": allocated: " + (HexToolkit.isBitSet(SSTContent[i], 6) ? '1' : '0') + ", activated: " + (HexToolkit.isBitSet(SSTContent[i], 7) ? '1' : '0'));
        }
    }

    public static byte[] readADN() throws CardException {

        if (DEBUG) {
            System.out.println(LoggingUtils.formatDebugMessage("reading EF_ADN file"));
        }

        SimCardFile file;

        try {
            file = FileManagement.selectPath("3f007f106f3a");
        } catch (FileNotFoundException e) {
            file = null;
        }

        if (null != file) { // in case there's a problem reading a file we don't wanna throw exception but rather continue with other files/actions

            int nrOfRecords = ((SimCardLinearFixedFile) file).getNumberOfRecords();
            int recordLength = ((SimCardLinearFixedFile) file).getRecordLength();
            int alphaLength = recordLength - 14;

            if (DEBUG) {
                System.out.println(LoggingUtils.formatDebugMessage("Record length: " + recordLength + ", Alpha ID length: " + alphaLength));
            }
            
            for (int i = 1; i <= nrOfRecords; i++) {
                byte[] content = ((SimCardLinearFixedFile) file).getRecord(i);
                boolean empty = true;

                for (int j = 0; j < recordLength; j++) {
                    if ((byte) content[j] != (byte) 0xFF) {
                        empty = false;
                    }
                }

                if (!empty) {
                    System.out.println("Record " + i + ": " + HexToolkit.toString(content));
                }
            }

            return null;

        } else {
            return null;
        }
    }

    public static byte[] readRawIMSI() throws CardException {

        if (DEBUG) {
            System.out.println(LoggingUtils.formatDebugMessage("reading EF_IMSI file"));
        }

        SimCardFile file;

        try {
            file = FileManagement.selectPath("3f007f206f07");
        } catch (FileNotFoundException e) {
            file = null;
        }

        if (null != file) { // in case there's a problem reading a file we don't wanna throw exception but rather continue with other files/actions

            byte[] content = ((SimCardTransparentFile) file).getContent();
            if (null != content) {
                int length = content[0];

                if (content.length - 1 != length) {
                    System.err.println(LoggingUtils.formatDebugMessage("readIMSI: Length of data is somehow messaged up compared to length defined as 1st byte of EF_IMSI"));
                    return null;
                }

                return content;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public static String swapIMSI(byte[] content) throws CardException {

        byte[] swapped = new byte[content.length - 1];

        for (int i = 0; i < content.length - 1; i++) {
            swapped[i] = HexToolkit.swap(content[i + 1]);
            if (i == 0) {
                swapped[i] = (byte) ((short) swapped[i] | (short) 0xF0);
            }
        }

        return HexToolkit.stripChars(HexToolkit.toString(swapped));
    }

    public static String readICCID() throws CardException {

        if (DEBUG) {
            System.out.println(LoggingUtils.formatDebugMessage("reading EF_ICCID file"));
        }

        SimCardFile file;

        try {
            file = FileManagement.selectPath("3f002fe2");
        } catch (FileNotFoundException e) {
            file = null;
        }

        if (null == file) {
            return null;
        }

        byte[] content = ((SimCardTransparentFile) file).getContent();

        byte[] swapped = new byte[content.length];

        for (int i = 0; i < content.length; i++) {
            swapped[i] = HexToolkit.swap(content[i]);
        }

        return HexToolkit.stripChars(HexToolkit.toString(swapped));
    }

    public static String readMANUAREA() throws CardException {

        if (DEBUG) {
            System.out.println(LoggingUtils.formatDebugMessage("reading EF_MANUAREA file"));
        }

        SimCardFile file;

        try {
            file = FileManagement.selectPath("3f000002");
        } catch (FileNotFoundException e) {
            file = null;
        }

        if (null != file) { // in case there's a problem reading a file we don't wanna throw exception but rather continue with other files/actions
            byte[] content = ((SimCardTransparentFile) file).getContent();
            if (null != content) {
                return HexToolkit.toString(content);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public static String readDIR() throws CardException {

        if (DEBUG) {
            System.out.println(LoggingUtils.formatDebugMessage("reading EF_DIR file"));
        }

        SimCardFile file;

        try {
            file = FileManagement.selectPath("3f002f00");
        } catch (FileNotFoundException e) {
            file = null;
        }

        if (null != file) { // in case there's a problem reading a file we don't wanna throw exception but rather continue with other files/actions
            if (file instanceof SimCardLinearFixedFile) {
                byte[] recordContent = ((SimCardLinearFixedFile) file).getFirstRecord();
                if (null != recordContent) {
                    return HexToolkit.toString(recordContent);
                } else {
                    return null;
                }
            } else {
                return null;
            }
        } else {
            return null;
        }
    }
}
