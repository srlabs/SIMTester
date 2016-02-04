package de.srlabs.simlib;

public class SelectResponse3G implements SelectResponse {

    private final static boolean LOCAL_DEBUG = false;
    private final static boolean DEBUG = Debug.DEBUG || LOCAL_DEBUG;
    private static byte _file_type;
    private static byte _ef_type;
    private static byte[] _response_data;
    private static byte[] _file_id;
    private static int _file_size;

    public SelectResponse3G(byte[] responseData) throws IllegalArgumentException {

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

        if (HexToolkit.isBitSet(file_desc[2], 7)) {
            _file_type = SimCardFile.RFU;
        } else if ((byte) (file_desc[2] & (byte) 0x38) == (byte) 0x38) {
            _file_type = SimCardFile.DF;
        } else if ((byte) (file_desc[2] & (byte) 0x38) == (byte) 0x0) {
            _file_type = SimCardFile.EF;
        }

        if (_file_type == SimCardFile.EF) {
            if ((byte) (file_desc[2] & (byte) 0x7) == (byte) 0x1) {
                _ef_type = SimCardElementaryFile.EF_TRANSPARENT;
            } else if ((byte) (file_desc[2] & (byte) 0x7) == (byte) 0x2) {
                _ef_type = SimCardElementaryFile.EF_LINEAR_FIXED;
            } else if ((byte) (file_desc[2] & (byte) 0x7) == (byte) 0x6) {
                _ef_type = SimCardElementaryFile.EF_CYCLIC;
            }

        }

        byte[] file_id_data = TLVToolkit.getTLV(responseData, (byte) 0x83, (byte) 0x82);
        if (DEBUG) {
            System.out.println(LoggingUtils.formatDebugMessage("File Identifier data (0x83): " + HexToolkit.toString(file_id_data)));
        }

        _file_id = new byte[]{(byte) file_id_data[2], (byte) file_id_data[3]};

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
        if (getFileType() != SimCardFile.EF) {
            throw new IllegalStateException("File is not an EF!");
        }
        return _ef_type;
    }

    @Override
    public int getFileSize() {
        return _file_size;
    }

}
