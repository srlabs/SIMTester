package de.srlabs.simlib;

public interface SelectResponse {

    public String getFileId();
    public byte[] getResponseData();
    public byte getFileType();
    public byte getEFType();
    public int getFileSize();

}
