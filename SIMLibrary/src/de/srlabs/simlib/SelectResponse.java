package de.srlabs.simlib;

public class SelectResponse {

    private String _fileId;
    private byte[] _responseData;

    public SelectResponse(byte[] responseData, String fileId) throws IllegalArgumentException {
        _fileId = HexToolkit.toString(new byte[]{(byte) responseData[4], (byte) responseData[5]});
        if (!fileId.equals(_fileId)) {
            throw new IllegalArgumentException("Response Data (" + HexToolkit.toString(responseData) + ") you've provided doesn't seem to correspond to the fileId provided (" + fileId + ")");
        }
        _responseData = responseData;
    }

    public String getFileId() {
        return _fileId;
    }

    public byte[] getResponseData() {
        return _responseData;
    }

    public byte getFileType() {
        return _responseData[6];
    }
}
