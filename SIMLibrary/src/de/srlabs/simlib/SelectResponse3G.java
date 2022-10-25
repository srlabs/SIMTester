package de.srlabs.simlib;

import javax.smartcardio.CardException;

public class SelectResponse3G implements SelectResponse {

    private final static boolean LOCAL_DEBUG = false;
    private final static boolean DEBUG = Debug.DEBUG || LOCAL_DEBUG;
    private static byte _file_type;
    private static byte _ef_type;
    private static byte[] _response_data;
    private static byte[] _file_id;
    private static int _file_size;

    public SelectResponse3G(byte[] responseData) throws IllegalArgumentException, CardException {

        if (DEBUG) {
            System.out.println(LoggingUtils.formatDebugMessage("Parsing responseData: " + HexToolkit.toString(responseData)));
        }

        if (responseData[0] != (byte) 0x62) {
            throw new IllegalArgumentException("3G response has to start with 0x62 (FCP template tag). Your response starts with " + HexToolkit.toString(responseData[0]));
        }

        _response_data = responseData;

        byte[] file_desc = TLVToolkit.getTLV(responseData, (byte) 0x82);
        if (DEBUG) {
            System.out.println(LoggingUtils.formatDebugMessage("File Descriptor data (0x82): " + HexToolkit.toString(file_desc)));
        }

        // "DF name is mandatory for only ADF"
        // This means that if a df_name is specified, the responseData comes from an ADF
        byte[] df_name = TLVToolkit.getTLV(responseData, (byte) 0x84, (byte) 0x82);

        if (df_name != null) {
            _file_type = SimCardFile.ADF;
        } else if (HexToolkit.isBitSet(file_desc[2], 7)) {
            _file_type = SimCardFile.RFU;
        } else if ((byte) (file_desc[2] & (byte) 0x38) == (byte) 0x38 && (byte) (file_desc[2] | (byte) 0x78) == (byte) 0x78) {
            _file_type = SimCardFile.DF;
        } else if ((byte) (file_desc[2] | (byte) 0x47) == (byte) 0x47) {
            _file_type = SimCardFile.EF;
        } else if ((byte) (file_desc[2] & (byte) 0x08) == (byte) 0x08) {
            _file_type = SimCardFile.INTERNAL_EF;
        } else if ((byte) (file_desc[2] & (byte) 0x39) == (byte) 0x39 && (byte) (file_desc[2] | (byte) 0x79) == (byte) 0x79) {
            // Checks for BER-TLV EF structure...
            _file_type = SimCardFile.EF;
        } else {
            _file_type = SimCardFile.RFU;
        }

        if (_file_type == SimCardFile.EF || _file_type == SimCardFile.INTERNAL_EF) {
            switch (file_desc[2] & 0x07) {
                case 0x00:
                    // No info provided
                    _ef_type = SimCardElementaryFile.EF_NO_INFO;
                    break;
                case 0x01:
                    // Transparent or BER-TLV structure
                    if ((file_desc[2] & 0x39) == 0x39) {
                        // BER-TLV structure
                        _ef_type = SimCardElementaryFile.EF_BER_TLV;
//                        throw new CardException("BER-TLV structure found. File Descriptor data: " + HexToolkit.toString(responseData));
                    }
                    else {
                        // Transparent structure
                        _ef_type = SimCardElementaryFile.EF_TRANSPARENT;
                    }
                    break;
                case 0x02:
                    // Linear fixed structure
                    _ef_type = SimCardElementaryFile.EF_LINEAR_FIXED;
                    break;
                case 0x06:
                    // Cyclic structure
                    _ef_type = SimCardElementaryFile.EF_CYCLIC;
                    break;
                default:
                    // RFU
                    throw new CardException("RFU EF structure found. File Descriptor data: " + HexToolkit.toString(responseData));
            }
        }

        byte[] file_id_data = TLVToolkit.getTLV(responseData, (byte) 0x83, (byte) 0x82);
        if (DEBUG) {
            System.out.println(LoggingUtils.formatDebugMessage("File Identifier data (0x83): " + HexToolkit.toString(file_id_data)));
        }

        if (file_id_data != null) {
            _file_id = new byte[]{ file_id_data[2], file_id_data[3]};
        }

        byte[] file_size_data = TLVToolkit.getTLV(responseData, (byte) 0x80, (byte) 0x82);
        if (DEBUG) {
            System.out.println(LoggingUtils.formatDebugMessage("File Size data (0x80): " + HexToolkit.toString(file_size_data)));
        }

        if (null == file_size_data) {
            _file_size = -1;
        } else if ((byte) file_size_data[1] == 1) {
            _file_size = (byte) (file_size_data[2] & 0xFF);
        } else {
            _file_size = ((short) ((file_size_data[2] & 0xFF) << 8) | (file_size_data[3] & 0xFF));
        }
    }

    @Override
    public String getFileId() {
        return HexToolkit.toString(_file_id);
    }

    @Override
    public byte[] getResponseData() {
        return _response_data;
    }

    @Override
    public byte getFileType() {
        return _file_type;
    }

    @Override
    public byte getEFType() {
        if (getFileType() != SimCardFile.EF && getFileType() != SimCardFile.INTERNAL_EF) {
            throw new IllegalStateException("File is not an EF!");
        }
        return _ef_type;
    }

    @Override
    public int getFileSize() {
        return _file_size;
    }

}
