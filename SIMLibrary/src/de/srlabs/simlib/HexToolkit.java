package de.srlabs.simlib;

public class HexToolkit {

    public static byte[] fromString(String byteString) {
        byte result[] = new byte[byteString.length() / 2];
        for (int i = 0; i < byteString.length(); i += 2) {
            String toParse = byteString.substring(i, i + 2);
            result[i / 2] = (byte) (Integer.parseInt(toParse, 16) & 0xff);
        }

        return result;
    }

    public static byte fromStringToSingleByte(String byteString) {
        return (byte) (Integer.parseInt(byteString, 16) & 0xff);
    }

    public static String toString(byte[] data, int offset, int len) {
        if (null == data) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            sb.append(toString(data[offset + i]));
        }

        return sb.toString();
    }

    public static String toString(byte b) {
        return toHexString((long) b & 255L, 1);
    }

    public static String toString(int i) {
        if (i <= 255) {
            return toString((byte) i);
        } else {
            // FIXME: more than one-byte integers should be convertable too!
            throw new IllegalArgumentException("This is not implemented yet, only i < 255 is convertable for now!");
        }
    }

    public static String toHexString(long l, int len) {
        StringBuilder result;
        for (result = new StringBuilder(Long.toHexString(l).toUpperCase()); result.length() < 2 * len; result.insert(0, "0"));
        return result.toString();
    }

    public static String toString(byte[] data) {
        if (null == data) {
            return null;
        }
        return toString(data, 0, data.length);
    }

    public static String toText(byte[] data) {
        StringBuilder text = new StringBuilder();

        for (int i = 0; i < data.length; i++) {
            text.append((char) data[i]);
        }

        return text.toString();
    }

    /**
     * byte swap for 1 byte
     *
     * @param value Value to byte swap.
     * @return Byte swapped representation.
     */
    public static byte swap(byte value) {
        return (byte) ((short) ((short) (value & (short) 0x0F) << 4) + (short) ((short) (value & (short) 0xF0) >> 4));
    }

    public static String stripChars(String input) {
        StringBuilder strBuff = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            if (Character.isDigit(input.charAt(i))) {
                strBuff.append(input.charAt(i));
            }
        }
        return strBuff.toString();
    }

    public static boolean isBitSet(byte value, int bitindex, int startbit) {
        return (value & (1 << bitindex - startbit)) != 0;
    }

    public static boolean isBitSet(byte value, int bitindex) { //numbered from b0-b7
        return isBitSet(value, bitindex, 0);
    }

    public static String printByteAsBites(byte value) {
        StringBuilder sb = new StringBuilder();
        for (int i = 7; i >= 0; i--) {
            sb.append((isBitSet(value, i) ? '1' : '0'));
        }
        return sb.toString();
    }

    public static boolean isByteArrayNullBytesOnly(byte[] bytearray) {
        if (null == bytearray) {
            return false;
        }

        boolean result = true;
        for (byte onebyte : bytearray) {
            if ((byte) onebyte != (byte) 0x00) {
                result = false;
            }
        }
        return result;
    }

    public static int indexOfByteArrayInByteArray(byte[] data, byte[] pattern) {
        int[] failure = computeFailure(pattern);

        int j = 0;
        if (data.length == 0) {
            return -1;
        }

        for (int i = 0; i < data.length; i++) {
            while (j > 0 && pattern[j] != data[i]) {
                j = failure[j - 1];
            }
            if (pattern[j] == data[i]) {
                j++;
            }
            if (j == pattern.length) {
                return i - pattern.length + 1;
            }
        }
        return -1;
    }

    private static int[] computeFailure(byte[] pattern) {
        int[] failure = new int[pattern.length];

        int j = 0;
        for (int i = 1; i < pattern.length; i++) {
            while (j > 0 && pattern[j] != pattern[i]) {
                j = failure[j - 1];
            }
            if (pattern[j] == pattern[i]) {
                j++;
            }
            failure[i] = j;
        }

        return failure;
    }

    public static int compareTARs(byte[] left, byte[] right) {
        if (left.length > 3 || right.length > 3) {
            throw new IllegalArgumentException("Unable to compare TARs as they're bigger than 3 bytes, this should never happen, report a bug.");
        }

        return new Long(byteArrayToLong(left) - byteArrayToLong(right)).intValue();
    }

    public static long byteArrayToLong(byte[] ba) {
        long value = 0;
        for (int i = 0; i < ba.length; i++) {
            value = (value << 8) + (ba[i] & 0xff);
            assert ((value & 0x80000000) == 0);
        }
        return value;
    }
}
