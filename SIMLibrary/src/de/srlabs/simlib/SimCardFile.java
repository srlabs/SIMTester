package de.srlabs.simlib;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public abstract class SimCardFile {

    private String _fileName = "N/A";
    private String _fileDescription = "N/A";
    private String _filePath;   // Defines the path of the file. e.g.: 3f00/2f00

    protected int _fileSize;
    protected String _fileId;
    protected byte _fileType;
    protected byte[] _selectResponseData;
    protected byte _ef_type;
    public final static byte RFU = (byte) 0x00;
    public final static byte MF = (byte) 0x01;
    public final static byte DF = (byte) 0x02;
    public final static byte EF = (byte) 0x04;
    public final static byte INTERNAL_EF = (byte) 0x08;
    public final static byte ADF = (byte) 0x09;

    protected SimCardFile(SelectResponse selectResponse) {
        _fileId = selectResponse.getFileId();
        _selectResponseData = selectResponse.getResponseData();
        _fileSize = selectResponse.getFileSize();
        _fileType = selectResponse.getFileType();
        if (_fileType == SimCardFile.EF) {
            _ef_type = selectResponse.getEFType();
        }
    }

    public int getFileSize() {
        return _fileSize;
    }

    public String getFileId() {
        return _fileId;
    }

    public byte getFileType() {
        return (byte) _fileType;
    }

    public String getFileTypeName() {
        switch (_fileType) {
            case RFU:
                return "RFU";
            case MF:
                return "MF";
            case DF:
                return "DF";
            case ADF:
                return "ADF";
            case EF:
            case INTERNAL_EF:
                switch ((byte) _ef_type) { // fileStructure byte
                    case SimCardElementaryFile.EF_TRANSPARENT:
                        return "EF_TRANSPARENT";
                    case SimCardElementaryFile.EF_LINEAR_FIXED:
                        return "EF_LINEAR";
                    case SimCardElementaryFile.EF_CYCLIC:
                        return "EF_CYCLIC";
                }
        }

        return "N/A";
    }

    public int getNumberOfChildDFs() {
        if (_fileType != MF && _fileType != DF && _fileType != ADF) {
            throw new IllegalStateException("Unable to get number of child DFs for " + _fileId + " as it's not a DF, nor MF");
        }

        if (SIMLibrary.third_gen_apdu) {
            // FIXME: really? 3G APDU don't give you the no of files and dirs?
            System.err.println(LoggingUtils.formatDebugMessage("3G format not yet implemented!"));
            return 0;
        } else {
            return (int) _selectResponseData[14] & 0xff; // we want integer
        }
    }

    public int getNumberOfChildEFs() {

        if (SIMLibrary.third_gen_apdu) {
            System.err.println(LoggingUtils.formatDebugMessage("3G format not yet implemented!"));
        }

        if (_fileType != MF && _fileType != DF && _fileType != ADF) {
            throw new IllegalStateException("Unable to get number of child DFs for " + _fileId + " as it's not a DF, nor MF");
        } else {
            return (int) _selectResponseData[15] & 0xff; // we want integer
        }
    }

    public byte[] getRawSelectResponseData() {
        return _selectResponseData;
    }

    public String getFileName() { return _fileName; }
    protected void setFileName(String name) { _fileName = name ;}

    public String getFileDescription() { return _fileDescription; }
    protected void setFileDescription(String desc) { _fileDescription = desc ;}

    public String getFilePath() { return _filePath; }
    protected void setFilePath(String name) { _filePath = name ;}
}
