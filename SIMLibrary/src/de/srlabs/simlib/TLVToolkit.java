package de.srlabs.simlib;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class TLVToolkit {

    public static byte[] getTLV(byte[] tlv, int tag) {
        if (tlv == null || tlv.length < 1) {
            throw new IllegalArgumentException("Invalid TLV");
        }

        int c = 0;
        byte[] TLV = null;

        ByteArrayInputStream is = null;
        try {
            is = new ByteArrayInputStream(tlv);

            while ((c = is.read()) != -1) {
                if (c == tag) {
                    if ((c = is.read()) != -1) {
                        TLV = new byte[c + 2];
                        TLV[0] = (byte) tag;
                        TLV[1] = (byte) c;
                        is.read(TLV, 2, c);
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
