package de.srlabs.simlib;

public abstract class SimCardElementaryFile extends SimCardFile {

    private int _fileStructure;
    public final static int EF_TRANSPARENT = (byte) 0x00;
    public final static int EF_LINEAR_FIXED = (byte) 0x01;
    public final static int EF_CYCLIC = (byte) 0x03;
    public final static int EF_NO_INFO = (byte) 0x04;
    public final static int EF_BER_TLV = (byte) 0x05;

    public SimCardElementaryFile(SelectResponse selectResponse) {
        super(selectResponse);

        if (getFileType() == EF || getFileType() == INTERNAL_EF) {
            _fileStructure = selectResponse.getEFType();
        } else {
            throw new UnsupportedOperationException("Selected file is not an EF, unable to get EF structure");
        }
    }

    public int getFileStructure() throws UnsupportedOperationException {
        return _fileStructure;
    }

    public String getFileStructureName() {
        String name = "UNKNOWN";

        switch (_fileStructure) {
            case EF_TRANSPARENT:
                name = "Transparent";
                break;
            case EF_LINEAR_FIXED:
                name = "Linear-fixed";
                break;
            case EF_CYCLIC:
                name = "Cyclic";
                break;
        }

        return name;
    }
}
