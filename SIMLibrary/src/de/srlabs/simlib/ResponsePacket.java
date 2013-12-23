package de.srlabs.simlib;

import java.util.Arrays;

public class ResponsePacket {

    // 02 71 00 0013 12             B00010 0000000001 00 01 AABBCCDDEEFFAABB - standard 03.48 Response Packet
    // 02 7F 00 0016 15 00 00 A9 A9 B00010 0000000001    09 AABBCCDDEEFFAABB - some proprietary RP
    /* 03.48 variables for RP */
    public static byte[] RPH = new byte[]{(byte) 0x02, (byte) 0x71, (byte) 0x00}; // Response Packet Header - 3 bytes
    public static byte[] RPH2 = new byte[]{(byte) 0x02, (byte) 0x7F, (byte) 0x00}; // Response Packet Header - 3 bytes
    private byte[] RPL = new byte[2]; // Response Packet Length - 2 bytes
    private byte RHL; // Response Header Length - 1 byte
    private byte[] TAR = null; // Target Application Identifier - 3 bytes
    private byte[] CNTR = null; // Counter - 5 bytes
    private byte PCNTR = (byte) 0x00; // Padding counter, no padding by default - 1 byte
    private byte RSC; // Response Status code - 1 byte
    private byte[] CC = null; // Cryptographic checksum - optional (if previously requested) - 8 bytes if present
    private byte[] ARD = null; // Additional Response data - optional - X bytes if present
    private boolean _cc_present = false;
    private boolean _ard_present = false;
    private boolean _is_encrypted = false;
    private byte[] _bytes;
    private long _originalCounter;

    public ResponsePacket() {
    }

    public void parse(byte[] data) {
        parse(data, -1);
    }

    public void parse(byte[] data, long originalCounter) {
        if (data.length < 16) {
            // RPH (3 bytes) + RPL (2 bytes) + RHL (1 byte) + TAR (3 bytes) + CNTR (5 bytes) + PCNTR (1 bytes) + RSC (1 byte)
            System.err.println(LoggingUtils.formatDebugMessage("Data provided don't seem to be valid, data should be at least 16 bytes long for a valid ResponsePacket (" + HexToolkit.toString(data) + ")"));
        }

        _bytes = data;
        _originalCounter = originalCounter;

        if (Arrays.equals(Arrays.copyOfRange(data, 0, 3), RPH2)) {
            RSC = (byte) data[18];
            System.out.println(LoggingUtils.formatDebugMessage("Proprietary RP found (0x027F00), bypassing everything, setting PoR code only: 0x" + HexToolkit.toString(RSC)));
            return;
        } else if (Arrays.equals(Arrays.copyOfRange(data, 0, 3), RPH)) {
            // all good, standard PoR code
        } else {
            System.err.println(LoggingUtils.formatDebugMessage("Response Packet Header (RPH) not found in the data provided! Your RPH is: " + HexToolkit.toString(Arrays.copyOfRange(data, 0, 3))));
        }

        int _rp_length = data.length - RPH.length - RPL.length; // should be minus 5 (3 bytes RPH and 2 bytes RPL)
        int _RPL_data_length = (byte) ((data[3] & 0xff) << 8) | (byte) (data[4] & 0xff);

        if (_rp_length != _RPL_data_length) {
            System.err.println(LoggingUtils.formatDebugMessage("Response packet length (RPL) doesn't correspond with the actual data length; real length = " + _rp_length + "; RPL = " + _RPL_data_length));
        }

        RPL[0] = (byte) data[3];
        RPL[1] = (byte) data[4];

        RHL = (byte) data[5];

        if (RHL == 10 | RHL == 18) {
            TAR = new byte[3];
            System.arraycopy(data, 6, TAR, 0, 3);
            CNTR = new byte[5];
            System.arraycopy(data, 9, CNTR, 0, 5);
            PCNTR = (byte) data[14];
            RSC = (byte) data[15];
            if (RHL == 18) {
                CC = new byte[8];
                System.arraycopy(data, 16, CC, 0, 8);
                _cc_present = true;
            }
        } else {
            System.err.println(LoggingUtils.formatDebugMessage("Unexpected Response Header Length (RHL), should be 10 bytes without a CC or 18 bytes with a CC, current value: " + RHL));
        }

        if ((PCNTR & 0xFF) > (_RPL_data_length - RHL - 1)) {
            PCNTR = 0; // PNCTR is probably encrypted and therefore bullshit
        } else {
            boolean paddingBytesAreZeros = true;
            for (int i = 0; i < (PCNTR & 0xFF); i++) {
                if (data[16 + i] != (byte) 0x00) {
                    paddingBytesAreZeros = false;
                }
            }
            if (!paddingBytesAreZeros) {
                PCNTR = 0; // padding does not fit the padding counter (PCNTR)
            }
        }

        if ((_RPL_data_length - PCNTR - RHL) > 1) {
            _ard_present = true;
            // there are additional data in the Response packet
            int _additional_data_length = _RPL_data_length - PCNTR - RHL - 1;
            ARD = new byte[_additional_data_length];
            System.arraycopy(data, (_cc_present) ? 24 : 16, ARD, 0, _additional_data_length);
        }
        if (originalCounter != -1) {
            // ARD must be present because otherwise CNTR might not be equal to origCNTR if encryption was used on commandpacket
            if (originalCounter != getCounter() && areAdditionalDataPresent()) {
                _is_encrypted = true;
            }
        }
    }

    public byte[] getBytes() {
        return _bytes;
    }

    public boolean isCryptographicChecksumPresent() {
        return _cc_present;
    }

    public byte[] getCryptographicChecksum() {
        if (!isCryptographicChecksumPresent()) {
            throw new IllegalStateException("Cryptographic checksum is not present, you can't get it!");
        }

        return CC;
    }

    public byte getStatusCode() {
        return RSC;
    }

    public boolean isEncrypted() {
        if (_originalCounter == -1) {
            throw new IllegalStateException("Cannot determine if ResponsePacket is encrypted as original counter from CP was not provided, fix your code!");
        }
        return _is_encrypted;
    }

    public long getCounter() {
        long counter = 0;
        for (int i = 0; i < 5; i++) {
            counter <<= 8;
            counter ^= (long) CNTR[i] & 0xFF;
        }
        return counter;
    }

    public byte getPaddingCounter() {
        return PCNTR;
    }

    public boolean areAdditionalDataPresent() {
        return _ard_present;
    }

    public byte[] getAdditionalData() {
        return ARD;
    }

    public static class Helpers {

        public static byte[] findResponsePacket(byte[] data) {
            int index;

            if ((index = HexToolkit.indexOfByteArrayInByteArray(data, ResponsePacket.RPH)) != -1) {
                return Arrays.copyOfRange(data, index, data.length);
            }

            if ((index = HexToolkit.indexOfByteArrayInByteArray(data, ResponsePacket.RPH2)) != -1) {
                return Arrays.copyOfRange(data, index, data.length);
            }

            return null;
        }
    }
}
