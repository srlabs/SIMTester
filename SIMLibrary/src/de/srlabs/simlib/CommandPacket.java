package de.srlabs.simlib;

import java.text.ParseException;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class CommandPacket {

    private final static boolean LOCAL_DEBUG = false;
    private final static boolean DEBUG = Debug.DEBUG || LOCAL_DEBUG;
    private boolean _fake3DES = false;
    private byte[] _bytes = null;
    /* 03.48 variables for CP */
    private byte[] CPH = new byte[]{(byte) 0x02, (byte) 0x70, (byte) 0x00}; // Command Packet Header
    private byte[] CPL = new byte[2]; // Command Packet Length
    private byte CHL; // Command Header Length
    /* 03.48, Section 5.1.1 Coding of the SPI */
    private byte SPI1 = (byte) 0x00;
    private Byte fakeSPI1;
    /* 03.48, Section 5.1.1 Coding of the SPI */
    private byte SPI2 = (byte) 0x00;
    private Byte fakeSPI2;
    /* 03.48, Section 5.1.2 Coding of the KIc */
    private byte KIC;
    private Byte fakeKIC;
    /* 03.48, Section 5.1.3 Coding of the KID */
    private byte KID;
    private Byte fakeKID;
    private byte[] TAR = null; // Target Application Identifier
    private byte[] CNTR = null; // Counter
    private byte PCNTR = (byte) 0x00; // Padding counter, no padding by default
    private byte[] CC = null; // Cryptographic checksum
    private byte[] UD = null; // User data
    /* internal variables, not 03.48 related */
    private int _KIC_implicit_algo = -1;
    private int _KID_implicit_algo = -1;
    private byte[] _kic_key;
    private byte[] _kid_key;
    private byte[] _encryptedCNTR;
    /* constants */
    // Counter management
    public final static byte CNTR_NO_CNTR_AVAILABLE = (byte) 0x0;
    public final static byte CNTR_CNTR_AVAILABLE = (byte) 0x1;
    public final static byte CNTR_CNTR_HIGHER = (byte) 0x2;
    public final static byte CNTR_CNTR_ONE_HIGHER = (byte) 0x3;
    // KIC
    public final static int KIC_ALGO_ERROR_UNKNOWN = -1;
    public final static int KIC_ALGO_IMPLICIT = 0;
    public final static int KIC_ALGO_DES_CBC = 1;
    public final static int KIC_ALGO_DES_ECB = 4;
    public final static int KIC_ALGO_3DES_CBC_2KEYS = 2;
    public final static int KIC_ALGO_3DES_CBC_3KEYS = 3;
    // KID
    public final static int KID_ALGO_ERROR_UNKNOWN = -1;
    public final static int KID_ALGO_IMPLICIT = 0;
    public final static int KID_ALGO_DES_CBC = 1;
    public final static int KID_ALGO_3DES_CBC_2KEYS = 2;
    public final static int KID_ALGO_3DES_CBC_3KEYS = 3;
    // PoR Security
    public final static byte POR_SECURITY_NONE = (byte) 0x0;
    public final static byte POR_SECURITY_RC = (byte) 0x1;
    public final static byte POR_SECURITY_CC = (byte) 0x2;
    public final static byte POR_SECURITY_DS = (byte) 0x3;
    // PoR mode
    public final static byte POR_MODE_SMS_DELIVER_REPORT = (byte) 0x0;
    public final static byte POR_MODE_SMS_SUBMIT = (byte) 0x1;
    // idiotic Axalto
    private boolean _axaltoSignature = false;
    private boolean _reallyPerformEncryption = false;

    public CommandPacket() {
    }

    public byte getSPI1() {
        return SPI1;
    }

    public byte getSPI2() {
        return SPI2;
    }

    public byte getKIC() {
        return KIC;
    }

    public byte getKID() {
        return KID;
    }

    public void setCPH(byte[] cph) {
        CPH = cph;
    }

    /* SPI1 settings */
    public boolean isCryptographicChecksumEnabled() {
        return HexToolkit.isBitSet(SPI1, 1);
    }

    public void setCryptographicChecksum(boolean enabled, byte[] kid_key) {
        if (enabled) {
            SPI1 = (byte) ((byte) SPI1 | (byte) 0x2);
            if (kid_key.length % 8 != 0) {
                throw new IllegalArgumentException("Key length is not a multiple of a block size (8 bytes)!");
            } else {
                _kid_key = kid_key;
            }
        } else {
            SPI1 = (byte) ((byte) SPI1 & (byte) 0xFD);
            _kid_key = new byte[0];
        }
    }

    public boolean isCipheringEnabled() {
        return (byte) ((byte) SPI1 & (byte) 0x4) == (byte) 0x4;
    }

    public void setCiphering(boolean enabled, byte[] kic_key) {
        System.out.println("WARNING: Encryption will NOT be really performed (just SPI and KIC will be set), use reallyPerformEncryption = true");
        setCiphering(enabled, kic_key, false);
    }

    public void setCiphering(boolean enabled, byte[] kic_key, boolean reallyPerformEncryption) {
        _reallyPerformEncryption = reallyPerformEncryption;
        if (enabled) {
            SPI1 = (byte) ((byte) ((byte) SPI1 & (byte) 0xFB) | (byte) 0x4);
            if (kic_key.length % 8 != 0) {
                throw new IllegalArgumentException("Key length is not a multiple of a block size (8 bytes)!");
            } else {
                _kic_key = kic_key;
            }
        } else {
            SPI1 = (byte) ((byte) SPI1 & (byte) 0xFB);
            _kic_key = new byte[0];
        }
    }

    public byte getCounterManagement() {
        return (byte) ((byte) ((byte) SPI1 & (byte) 0x18) >> 3);
    }

    public void setCounterManegement(byte counterManagement) {
        SPI1 = (byte) ((byte) SPI1 | (byte) ((byte) ((byte) counterManagement << 3) & (byte) 0x18));
    }

    /* SPI2 settings*/
    public boolean isPoREnabled() { // this implementation miss 0x10 (PoR required only when an error has occured) to ease up its implementation, it's not going to be possible to set it to this value in setPoR() method
        if ((byte) ((byte) SPI2 & (byte) 0x3) == (byte) 0x1) {
            return true;
        } else {
            return false;
        }
    }

    public void setPoR(boolean enabled) {
        if (enabled) {
            SPI2 = (byte) ((byte) ((byte) SPI2 & (byte) 0xFC) | (byte) 0x1);
        } else {
            SPI2 = (byte) ((byte) SPI2 & (byte) 0xFC);
        }
    }

    public byte getPoRSecurity() {
        return (byte) ((byte) ((byte) SPI2 & (byte) 0xC) >> 2);
    }

    public void setPoRSecurity(byte PoRSecurity) {
        SPI2 = (byte) ((byte) SPI2 | (byte) ((byte) ((byte) PoRSecurity << 2) & (byte) 0xC));
    }

    public boolean isPoRCipheringEnabled() {
        return (byte) ((byte) SPI2 & (byte) 0x10) == (byte) 0x10;
    }

    public void setPoRCiphering(boolean enabled) {
        if (enabled) {
            SPI2 = (byte) ((byte) ((byte) SPI2 & (byte) 0xEF) | (byte) 0x10);
        } else {
            SPI2 = (byte) ((byte) SPI2 & (byte) 0xEF);
        }
    }

    public byte getPoRMode() {
        return (byte) ((byte) ((byte) SPI2 & (byte) 0x20) >> 5);
    }

    public void setPoRMode(byte PoRMode) {
        SPI2 = (byte) ((byte) SPI2 | (byte) ((byte) ((byte) PoRMode << 5) & (byte) 0x20));
    }

    /* KIC/KID settings */
    public int getKeyset() {
        byte KIC_keyset = (byte) ((byte) ((byte) KIC >> 4) & (byte) 0x0F);
        byte KID_keyset = (byte) ((byte) ((byte) KID >> 4) & (byte) 0x0F);

        if (KIC_keyset != KID_keyset) {
            throw new IllegalStateException("KIC and KID keysets are not the same, wtf? KIC = " + HexToolkit.toString(KIC) + ", KID = " + HexToolkit.toString(KID));
        }

        return KIC_keyset;
    }

    public int getKICKeyset() {
        return (byte) ((byte) ((byte) KIC >> 4) & (byte) 0x0F);
    }

    public int getKIDKeyset() {
        return (byte) ((byte) ((byte) KID >> 4) & (byte) 0x0F);
    }

    public void setKeyset(int keyset) {
        KIC = (byte) ((byte) ((byte) KIC & (byte) 0x0F) | (byte) ((byte) keyset << 4));
        KID = (byte) ((byte) ((byte) KID & (byte) 0x0F) | (byte) ((byte) keyset << 4));
    }

    public void setKICKeyset(int keyset) {
        KIC = (byte) ((byte) ((byte) KIC & (byte) 0x0F) | (byte) ((byte) keyset << 4));
    }

    public void setKIDKeyset(int keyset) {
        KID = (byte) ((byte) ((byte) KID & (byte) 0x0F) | (byte) ((byte) keyset << 4));
    }

    public int getKICAlgo() {
        if ((byte) ((byte) KIC & (byte) 0x3) == (byte) 0x0) {
            return KIC_ALGO_IMPLICIT;
        } else if ((byte) ((byte) KIC & (byte) 0x3) == (byte) 0x1) {
            switch ((byte) ((byte) KIC & (byte) 0xC)) {
                case (byte) 0x0:
                    return KIC_ALGO_DES_CBC;
                case (byte) 0x4:
                    return KIC_ALGO_3DES_CBC_2KEYS;
                case (byte) 0x8:
                    return KIC_ALGO_3DES_CBC_3KEYS;
                case (byte) 0xC:
                    return KIC_ALGO_DES_ECB;
                default:
                    return KIC_ALGO_ERROR_UNKNOWN;
            }
        } else {
            return KIC_ALGO_ERROR_UNKNOWN;
        }
    }

    public String getKICAlgoName() {
        if ((byte) ((byte) KIC & (byte) 0x3) == (byte) 0x0) {
            return "IMPLICIT";
        } else if ((byte) ((byte) KIC & (byte) 0x3) == (byte) 0x1) {
            switch ((byte) ((byte) KIC & (byte) 0xC)) {
                case (byte) 0x0:
                    return "1DES-CBC";
                case (byte) 0x4:
                    return "3DES-2keys";
                case (byte) 0x8:
                    return "3DES-3keys";
                case (byte) 0xC:
                    return "1DES-ECB";
                default:
                    return "N/A";
            }
        } else {
            return "N/A";
        }
    }

    public void setKICAlgo(int algo) {
        switch (algo) {
            case KIC_ALGO_IMPLICIT:
                KIC = (byte) ((byte) KIC & (byte) 0xF0);
                break;
            case KIC_ALGO_DES_CBC:
                KIC = (byte) ((byte) ((byte) KIC & (byte) 0xF0) | (byte) 0x1);
                break;
            case KIC_ALGO_3DES_CBC_2KEYS:
                KIC = (byte) ((byte) ((byte) KIC & (byte) 0xF0) | (byte) 0x5);
                break;
            case KIC_ALGO_3DES_CBC_3KEYS:
                KIC = (byte) ((byte) ((byte) KIC & (byte) 0xF0) | (byte) 0x9);
                break;
            case KIC_ALGO_DES_ECB:
                KIC = (byte) ((byte) ((byte) KIC & (byte) 0xF0) | (byte) 0xD);
                break;
        }
    }

    public int getKIDAlgo() {
        if ((byte) ((byte) KID & (byte) 0x3) == (byte) 0x0) {
            return KID_ALGO_IMPLICIT;
        } else if ((byte) ((byte) KID & (byte) 0x3) == (byte) 0x1) {
            switch ((byte) ((byte) KID & (byte) 0xC)) {
                case (byte) 0x0:
                    return KID_ALGO_DES_CBC;
                case (byte) 0x4:
                    return KID_ALGO_3DES_CBC_2KEYS;
                case (byte) 0x8:
                    return KID_ALGO_3DES_CBC_3KEYS;
                default:
                    return KID_ALGO_ERROR_UNKNOWN;
            }
        } else {
            return KID_ALGO_ERROR_UNKNOWN;
        }
    }

    public String getKIDAlgoName() {
        if ((byte) ((byte) KID & (byte) 0x3) == (byte) 0x0) {
            return "IMPLICIT";
        } else if ((byte) ((byte) KID & (byte) 0x3) == (byte) 0x1) {
            switch ((byte) ((byte) KID & (byte) 0xC)) {
                case (byte) 0x0:
                    return "1DES-CBC";
                case (byte) 0x4:
                    return "3DES-2keys";
                case (byte) 0x8:
                    return "3DES-3keys";
                default:
                    return "N/A";
            }
        } else {
            return "N/A";
        }
    }

    public void setKIDAlgo(int algo) {
        switch (algo) {
            case KID_ALGO_IMPLICIT:
                KID = (byte) ((byte) KID & (byte) 0xF0);
                break;
            case KID_ALGO_DES_CBC:
                KID = (byte) ((byte) ((byte) KID & (byte) 0xF0) | (byte) 0x1);
                break;
            case KID_ALGO_3DES_CBC_2KEYS:
                KID = (byte) ((byte) ((byte) KID & (byte) 0xF0) | (byte) 0x5);
                break;
            case KID_ALGO_3DES_CBC_3KEYS:
                KID = (byte) ((byte) ((byte) KID & (byte) 0xF0) | (byte) 0x9);
                break;
        }
    }

    public void setKIDImplicitAlgo(int algo) {
        if (algo != KID_ALGO_DES_CBC || algo != KID_ALGO_3DES_CBC_2KEYS || algo != KID_ALGO_3DES_CBC_3KEYS) {
            throw new IllegalArgumentException();
        } else {
            _KID_implicit_algo = algo;
        }
    }

    public byte[] getTAR() {
        return TAR;
    }

    public void setTAR(byte[] targetApplicationIdentifier) {
        TAR = new byte[3];
        if (targetApplicationIdentifier.length != 3) {
            throw new IllegalArgumentException("TAR you have specified has incorrect size (not 3 bytes)!");
        } else {
            TAR = targetApplicationIdentifier;
        }
    }

    public long getCounter() {
        if (this.isCipheringEnabled()) {
            return -1;
        }
        long counter = 0;
        for (int i = 0; i < 5; i++) {
            counter <<= 8;
            counter ^= (long) CNTR[i] & 0xFF;
        }
        return counter;
    }

    public long getPoRCounter() {
        if (this.isCipheringEnabled()) {
            long counter = 0;
            for (int i = 0; i < 5; i++) {
                counter <<= 8;
                counter ^= (long) _encryptedCNTR[i] & 0xFF;
            }
            return counter;
        } else {
            return getCounter();
        }
    }

    public void setCounter(long counter) {
        CNTR = new byte[5];
        for (int i = 0; i < 5; i++) {
            CNTR[4 - i] = (byte) (counter >>> (i * 8));
        }
    }

    public byte getPaddingCounter() {
        return PCNTR;
    }

    public byte[] getCryptographicChecksum() {
        return CC;
    }

    public byte[] getUserData() {
        return UD;
    }

    public void setUserData(byte[] userData) {
        UD = new byte[userData.length];
        UD = userData;
    }

    public byte[] getBytes() throws SecurityException {
        byte[] result;

        if (null != _bytes) {
            return _bytes;
        }

        if (getKICKeyset() > -1 && getKICKeyset() < 16 && getKIDKeyset() > -1 && getKIDKeyset() < 16) {
            if (null != TAR) {
                if (null != CNTR) {
                    if (null != UD) {
                        result = _formatMessage();
                    } else {
                        throw new IllegalStateException("You must set User Data before message formatting!");
                    }
                } else {
                    throw new IllegalStateException("You must set Counter before message formatting!");
                }
            } else {
                throw new IllegalStateException("You must set Target Application Identifier (TAR) before message formatting!");
            }
        } else {
            throw new IllegalStateException("You must set keyset / algorithm before message formatting!");
        }

        return result;
    }

    public boolean isAxaltoSignatureEnabled() {
        return _axaltoSignature;
    }

    public void setAxaltoSignature(boolean value) {
        _axaltoSignature = value;
    }

    private byte[] _formatMessage() throws SecurityException {
        // never call this method before checking for variables being set, this method has no internal checks!
        int _chl = 13; // 2b (SPI) + 1b (KIC) + 1b (KID) + 3b (TAR) + 5b (CNTR) + 1b (PCNTR)

        if (this.isCryptographicChecksumEnabled()) {
            _chl += 8;
        }

        if (this.isCipheringEnabled()) {
            _ciper_set_PCNTR();
        }

        int _cpl = 1 /* CHL itself */ + _chl + UD.length + (this.isCipheringEnabled() ? PCNTR : 0);

        CPL[0] = (byte) ((byte) _cpl >> 8);
        CPL[1] = (byte) _cpl;
        CHL = (byte) _chl;

        byte[] command_packet = new byte[CPH.length + CPL.length + _cpl];

        System.arraycopy(CPH, 0, command_packet, 0, CPH.length);
        System.arraycopy(CPL, 0, command_packet, CPH.length, CPL.length);
        command_packet[CPH.length + CPL.length] = CHL;
        command_packet[CPH.length + CPL.length + 1] = (null == fakeSPI1) ? SPI1 : fakeSPI1;
        command_packet[CPH.length + CPL.length + 2] = (null == fakeSPI2) ? SPI2 : fakeSPI2;
        command_packet[CPH.length + CPL.length + 3] = (null == fakeKIC) ? KIC : (byte) ((KIC & 0xF0) | fakeKIC);
        command_packet[CPH.length + CPL.length + 4] = (null == fakeKID) ? KID : (byte) ((KID & 0xF0) | fakeKID);
        System.arraycopy(TAR, 0, command_packet, CPH.length + CPL.length + 5, TAR.length);
        System.arraycopy(CNTR, 0, command_packet, CPH.length + CPL.length + 5 + TAR.length, CNTR.length);
        command_packet[CPH.length + CPL.length + 5 + TAR.length + CNTR.length] = PCNTR;

        int cc_offset = 0;

        if (this.isCryptographicChecksumEnabled()) {
            CC = new byte[8];
            CC = _calcCC();
            cc_offset = 8;
            System.arraycopy(CC, 0, command_packet, CPH.length + CPL.length + 5 + TAR.length + CNTR.length + 1, CC.length);
        }

        System.arraycopy(UD, 0, command_packet, CPH.length + CPL.length + 5 + TAR.length + CNTR.length + 1 + cc_offset, UD.length);

        if (this.isCipheringEnabled()) {
            byte[] ciphered = _cipher();
            if (_reallyPerformEncryption) {
                System.arraycopy(ciphered, 0, command_packet, CPH.length + CPL.length + 5 + TAR.length, ciphered.length); // same offset as CNTR
            }
        }

        return command_packet;
    }

    private void _ciper_set_PCNTR() {
        int data_length = CNTR.length + 1 + (this.isCryptographicChecksumEnabled() ? 8 : 0) + UD.length;
        PCNTR = (byte) ((8 - (data_length % 8)) % 8);
    }

    private byte[] _cipher() throws SecurityException {

        int data_length = CNTR.length + 1 + (this.isCryptographicChecksumEnabled() ? 8 : 0) + UD.length;

        _ciper_set_PCNTR();

        byte[] toBeCiphered = new byte[data_length + PCNTR];

        System.arraycopy(CNTR, 0, toBeCiphered, 0, CNTR.length);
        toBeCiphered[CNTR.length] = PCNTR;
        if (this.isCryptographicChecksumEnabled()) {
            System.arraycopy(CC, 0, toBeCiphered, CNTR.length + 1, CC.length);
            System.arraycopy(UD, 0, toBeCiphered, CNTR.length + 1 + CC.length, UD.length);
        } else {
            System.arraycopy(UD, 0, toBeCiphered, CNTR.length + 1, UD.length);
        }

        int kic_algo = this.getKICAlgo();
        String cipherName;
        String cipherMode;

        switch (kic_algo) {
            case KIC_ALGO_IMPLICIT:
                if (_KIC_implicit_algo == -1) {
                    throw new IllegalArgumentException("You need to set KIC implicit algo before using implicit value in KIC itself");
                } else {
                    kic_algo = _KIC_implicit_algo;
                    // we let the switch to go on to hit the correct algorithm below
                }
            case KIC_ALGO_DES_CBC:
                if (_kic_key.length != 8) {
                    throw new IllegalArgumentException("Key length for a single DES has to be 8 bytes!");
                }
                cipherName = "DES";
                cipherMode = "CBC";
                break;
            case KIC_ALGO_DES_ECB:
                if (_kic_key.length != 8) {
                    throw new IllegalArgumentException("Key length for a single DES has to be 8 bytes!");
                }
                cipherName = "DES";
                cipherMode = "ECB";
                break;
            case KIC_ALGO_3DES_CBC_2KEYS:
                if (_kic_key.length != 16 && _kic_key.length != 24) {
                    throw new IllegalArgumentException("Key length for a 3DES with 2 keys has to be either 16 or 24 bytes!");
                } else {
                    if (_kic_key.length == 16) {
                        byte[] tmpkey = _kic_key;
                        _kic_key = new byte[24];
                        System.arraycopy(tmpkey, 0, _kic_key, 0, tmpkey.length);
                        System.arraycopy(tmpkey, 0, _kic_key, tmpkey.length, 8); // repeat the first 8 bytes as the last key
                    }
                }
                cipherName = "DESede";
                cipherMode = "CBC";
                break;
            case KIC_ALGO_3DES_CBC_3KEYS:
                if (_kic_key.length != 24) {
                    throw new IllegalArgumentException("Key length for a 3DES with 3 keys has to be 24 bytes!");
                }
                cipherName = "DESede";
                cipherMode = "CBC";
                break;
            default:
                throw new IllegalArgumentException("Unknown algorithm for Cryptograpic checksum calculation, algo = " + this.getKIDAlgo());
        }

        try {
            Cipher cipher = Cipher.getInstance(cipherName + "/" + cipherMode + "/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(_kic_key, cipherName);
            IvParameterSpec iv = new IvParameterSpec(new byte[8]);

            cipher.init(1, keySpec, iv);
            if (DEBUG) {
                System.out.println(LoggingUtils.formatDebugMessage("toBeCiphered: " + HexToolkit.toString(toBeCiphered)));
            }
            byte[] ciphered = cipher.doFinal(toBeCiphered);
            _encryptedCNTR = new byte[CNTR.length];
            System.arraycopy(ciphered, 0, _encryptedCNTR, 0, CNTR.length);

            return ciphered;
        } catch (Exception e) {
            throw new SecurityException(e.fillInStackTrace());
        }
    }

    private byte[] _calcCC() throws SecurityException {
        byte[] cc = new byte[8];

        int _chl = 13; // 2b (SPI) + 1b (KIC) + 1b (KID) + 3b (TAR) + 5b (CNTR) + 1b (PCNTR)

        int data_length;
        int pompadlen;
        byte[] toBeSigned;

        if (_axaltoSignature && this.isCipheringEnabled()) {
            _ciper_set_PCNTR(); // set PCNTR and add padding bytes for encryption (because Axalto omg)
            data_length = CPL.length + 1 + _chl + UD.length;
            data_length += PCNTR; // PCNTR padding bytes added here are for encryption, you know.. because we calc CC here, so it's obvious
            pompadlen = (8 - (data_length % 8)) % 8; // this is for signature as normal ppl would do
            toBeSigned = new byte[data_length + pompadlen]; // so this is the length with 2*pom_pad len
        } else {
            data_length = CPL.length + 1 + _chl + UD.length;
            pompadlen = (8 - (data_length % 8)) % 8;
            toBeSigned = new byte[data_length + pompadlen];
        }

        System.arraycopy(CPL, 0, toBeSigned, 0, CPL.length);
        toBeSigned[CPL.length] = CHL;
        toBeSigned[CPL.length + 1] = SPI1;
        toBeSigned[CPL.length + 2] = SPI2;
        toBeSigned[CPL.length + 3] = KIC;
        toBeSigned[CPL.length + 4] = KID;
        System.arraycopy(TAR, 0, toBeSigned, CPL.length + 5, TAR.length);
        System.arraycopy(CNTR, 0, toBeSigned, CPL.length + 5 + TAR.length, CNTR.length);
        toBeSigned[CPL.length + 5 + TAR.length + CNTR.length] = PCNTR;
        System.arraycopy(UD, 0, toBeSigned, CPL.length + 5 + TAR.length + CNTR.length + 1, UD.length);
        // padding is solved by itself as it consists of 0x00 bytes which were written into the byte array during its initialization

        if (DEBUG) {
            System.out.println(LoggingUtils.formatDebugMessage("toBeSigned: " + HexToolkit.toString(toBeSigned)));
        }

        int kid_algo = this.getKIDAlgo();
        String cipherName;

        switch (kid_algo) {
            case KID_ALGO_IMPLICIT:
                if (_KID_implicit_algo == -1) {
                    throw new IllegalArgumentException("You need to set KID implicit algo before using implicit value in KID itself");
                } else {
                    kid_algo = _KID_implicit_algo;
                    // we let the switch to go on to hit the correct algorithm below
                }
            case KID_ALGO_DES_CBC:
                if (_kid_key.length != 8) {
                    throw new IllegalArgumentException("Key length for a single DES has to be 8 bytes!");
                }
                cipherName = "DES";
                break;
            case KID_ALGO_3DES_CBC_2KEYS:
                if (_kid_key.length != 16 && _kid_key.length != 24) {
                    throw new IllegalArgumentException("Key length for a 3DES with 2 keys has to be either 16 or 24 bytes!");
                } else {
                    if (_kid_key.length == 16) {
                        byte[] tmpkey = _kid_key;
                        _kid_key = new byte[24];
                        System.arraycopy(tmpkey, 0, _kid_key, 0, tmpkey.length);
                        System.arraycopy(tmpkey, 0, _kid_key, tmpkey.length, 8); // repeat the first 8 bytes as the last key
                    }
                }
                cipherName = "DESede";
                break;
            case KID_ALGO_3DES_CBC_3KEYS:
                if (_fake3DES) {
                    cipherName = "DES";
                    break;
                }
                if (_kid_key.length != 24) {
                    throw new IllegalArgumentException("Key length for a 3DES with 3 keys has to be 24 bytes!");
                }
                cipherName = "DESede";
                break;
            default:
                throw new IllegalArgumentException("Unknown algorithm for Cryptograpic checksum calculation, algo = " + this.getKIDAlgo());
        }

        try {
            Cipher cipher = Cipher.getInstance(cipherName + "/CBC/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(_kid_key, cipherName);
            IvParameterSpec iv = new IvParameterSpec(new byte[8]);

            cipher.init(1, keySpec, iv);

            byte[] signature = cipher.doFinal(toBeSigned);
            System.arraycopy(signature, signature.length - 8, cc, 0, 8);
        } catch (Exception e) {
            throw new SecurityException(e.fillInStackTrace());
        }
        return cc;
    }

    // 027000 0080 15 1601 25 25 0002037f3caa1c8c860e61cdc35c3bed8353d5e8c701b4dc1d7d21597c5194607782d51ec9d1ec2c5c7639fe700beaa86686058b12bcae25d2101004a5662a4155dbc497f0316f9b1cd76a712549724a69429b34b1fa3f986e4c8b8c257099f2cbdd15b57a2d390000000000000000000000000000000000000000
    public void parse(byte[] data) throws ParseException {

        if (!Arrays.equals(Arrays.copyOfRange(data, 0, 3), CPH)) {
            throw new ParseException("Command packet header not found in the data provided", 0);
        }

        _bytes = data;

        int _cp_length = data.length - CPH.length - CPL.length; // should be minus 5 (3 bytes RPH and 2 bytes RPL)
        int _CPL_data_length = (byte) ((data[3] & 0xff) << 8) | (byte) (data[4] & 0xff);

        if (_cp_length != _CPL_data_length) {
            throw new ParseException("Command packet length (CPL) doesn't correspond with the actual data length; real length = " + _cp_length + "; CPL = " + _CPL_data_length, 0);
        }

        // FIXME: check if CPL if 027000, if not then maybe we have concatenated header here - take concatenation in mind!
        CPL[0] = (byte) data[3];
        CPL[1] = (byte) data[4];

        CHL = (byte) data[5];

        // TODO: based on lengths find out what's inside CP (CC, DS, is it encrypted?, etc..)
        SPI1 = data[6];
        SPI2 = data[7];

        KIC = data[8];
        KID = data[9];

        TAR = Arrays.copyOfRange(data, 10, 13);
        _encryptedCNTR = Arrays.copyOfRange(data, 13, 18); // is this correct?

        if (!this.isCipheringEnabled()) {
            CNTR = Arrays.copyOfRange(data, 13, 18);
            PCNTR = data[18];
            if (this.isCryptographicChecksumEnabled()) {
                CC = Arrays.copyOfRange(data, 19, 19 + 8 /* length of a CC */);
                UD = Arrays.copyOfRange(data, 19 + 8 /* length of a CC */, data.length);
            } else {
                UD = Arrays.copyOfRange(data, 19, data.length);
            }
        }

        // TODO:
        // if CC is contained fill in CC (if not encrypted)
        // if encrypted make a variable for ciphertext and fill it (prepare getXXX, setXXX methods)
    }

    public void setFakeKIC(byte kic) {
        fakeKIC = kic;
    }

    public void setFakeKID(byte kid) {
        fakeKID = kid;
    }

    public void setFakeSPI1(byte spi1) {
        fakeSPI1 = spi1;
    }

    public void setFakeSPI2(byte spi2) {
        fakeSPI2 = spi2;
    }
}
