package de.srlabs.simlib;

public class SelectResponse2G implements SelectResponse {

    private final String _fileId;
    private byte[] _responseData;
    private int _fileSize;
    private byte _fileType;

    public SelectResponse2G(byte[] responseData, String fileId) throws IllegalArgumentException {
        _fileId = HexToolkit.toString(new byte[]{(byte) responseData[4], (byte) responseData[5]});
        if (!fileId.equals(_fileId)) {
            System.err.println("If this happened during file scanning (-sf), try to add the fileId " + fileId + " as a reserved fileId using -sfrv option.");
            throw new IllegalArgumentException("Response Data (" + HexToolkit.toString(responseData) + ") you've provided doesn't seem to correspond to the fileId provided (" + fileId + "), fileId in data: " + _fileId);
        }
        _responseData = responseData;
        _fileType = _responseData[6];

        if (_fileType == SimCardFile.MF || _fileType == SimCardFile.DF) {
            _fileSize = -1;
        } else {
            _fileSize = ((_responseData[2] & 0xFF) << 8) | (_responseData[3] & 0xFF);
        }
    }

    @Override
    public String getFileId() {
        return _fileId;
    }

    @Override
    public byte[] getResponseData() {
        return _responseData;
    }

    @Override
    public byte getFileType() {
        return _fileType;
    }

    @Override
    public byte getEFType() {
        if (getFileType() != SimCardFile.EF) throw new IllegalStateException("File is not an EF!");
        return _responseData[13];
    }

    @Override
    public int getFileSize() {
        return _fileSize;
    }
}
