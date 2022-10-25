package de.srlabs.simlib;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;

public class TLVToolkit {
    
    private final static boolean LOCAL_DEBUG = false;
    private final static boolean DEBUG = Debug.DEBUG || LOCAL_DEBUG;
    
    public static byte[] getTLV(byte[] tlv, byte tag) {
        return getTLV(tlv, tag, tag);
    }
    
    public static byte[] getTLV(byte[] tlv, byte tag, byte start_tlv) {
        if (DEBUG) {
            System.out.println(LoggingUtils.formatDebugMessage("TLV byte array: " + HexToolkit.toString(tlv) + ", looking for tag " + HexToolkit.toString(tag)));
        }
        
        if (tlv == null || tlv.length < 1) {
            throw new IllegalArgumentException("Invalid TLV. tlv=" + HexToolkit.toString(tlv));
        }
        
        int c = 0;
        byte[] TLV = null;
        
        ByteArrayInputStream is = null;
        try {
            is = new ByteArrayInputStream(tlv);
            
            while ((c = is.read()) != -1) {
                if ((byte) c == start_tlv) {
                    if (DEBUG) {
                        System.out.println(LoggingUtils.formatDebugMessage("Reading TLV (tag=" + HexToolkit.toString((byte) c) + ")"));
                    }
                    if ((c = is.read()) != -1) {
                        TLV = new byte[c + 2];
                        TLV[0] = (byte) start_tlv;
                        TLV[1] = (byte) c;
                        is.read(TLV, 2, c);
                        if ((byte) start_tlv == (byte) tag) {
                            break;
                        }
                        is.mark(-1);
                        start_tlv = (byte) is.read();
                        if (start_tlv == -1) return null;
                        is.reset();
                    }
                }
            }
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                }
            }
        }
        
        return TLV;
    }
}
