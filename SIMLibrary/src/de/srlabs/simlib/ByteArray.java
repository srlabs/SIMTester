package de.srlabs.simlib;

import java.util.Arrays;

public class ByteArray {

    private byte[] data;

    public ByteArray(byte[] input) {
        data = input;
    }

    public ByteArray(String input) {
        this(HexToolkit.fromString(input));
    }

    public byte[] getData() {
        return data;
    }

    public String getStringData() {
        return HexToolkit.toString(data);
    }

    public void setData(byte[] input) {
        data = input;
    }

    public void setStringData(String input) {
        data = HexToolkit.fromString(input);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(data);
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        ByteArray other = (ByteArray) obj;
        return Arrays.equals(data, other.data);
    }

}
