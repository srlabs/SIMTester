package de.srlabs.simlib;

import java.io.FileNotFoundException;
import javax.smartcardio.CardException;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

public class FileManagement {

    private final static boolean LOCAL_DEBUG = false;
    private final static boolean DEBUG = Debug.DEBUG || LOCAL_DEBUG;

    public static SimCardFile selectPath(String filePath) throws CardException, FileNotFoundException {

        if (filePath.length() % 4 != 0) {
            throw new IllegalArgumentException("filePath entered doesn't seem like a valid path (length is weird)");
        }

        byte[] filePath_bytes = HexToolkit.fromString(filePath);
        ResponseAPDU r;
        SimCardFile selectedFile = null;

        for (int i = 0; i < filePath_bytes.length; i += 2) {
            byte[] fileId = new byte[2];
            System.arraycopy(filePath_bytes, i, fileId, 0, 2);

            r = selectFileById(fileId);

            SelectResponse selectResponse;
            if (SIMLibrary.third_gen_apdu) {
                if ((short) 0x9000 == (short) r.getSW()) {
                    selectResponse = new SelectResponse3G(r.getData());
                } else if ((short) 0x6A82 == (short) r.getSW()) {
                    throw new FileNotFoundException("file ID: " + HexToolkit.toString(fileId) + "; doesn't seem to exist on this card; SW = " + Integer.toHexString(r.getSW()));
                } else {
                    throw new CardException("an unexpected error has occured during selection of file ID: " + HexToolkit.toString(fileId) + "; SW = " + Integer.toHexString(r.getSW()));
                }
            } else {
                if ((byte) 0x9f == (byte) r.getSW1()) {
                    selectResponse = getResponse2G(fileId, r.getSW2());
                } else if ((short) 0x9404 == (short) r.getSW()) {
                    throw new FileNotFoundException("file ID: " + HexToolkit.toString(fileId) + "; doesn't seem to exist on this card; SW = " + Integer.toHexString(r.getSW()));
                } else if ((short) 0x9000 == (short) r.getSW()) {
                    System.out.println(LoggingUtils.formatDebugMessage("w00t! fileId: " + HexToolkit.toString(fileId) + " returned 0x9000 with no additional data"));
                    return null;
                } else {
                    throw new CardException("an unexpected error has occured during selection of file ID: " + HexToolkit.toString(fileId) + "; SW = " + Integer.toHexString(r.getSW()));
                }
            }

            switch (selectResponse.getFileType()) { // fileType byte
                case SimCardFile.MF:
                    selectedFile = new SimCardMasterFile(selectResponse);
                    if (DEBUG) {
                        System.out.println(LoggingUtils.formatDebugMessage("selected MF " + selectResponse.getFileId() + ", child DFs: " + selectedFile.getNumberOfChildDFs() + ", child EFs: " + selectedFile.getNumberOfChildEFs()));
                    }
                    break;
                case SimCardFile.DF:
                    selectedFile = new SimCardDirectoryFile(selectResponse);
                    if (DEBUG) {
                        System.out.println(LoggingUtils.formatDebugMessage("selected DF " + selectResponse.getFileId() + ", child DFs: " + selectedFile.getNumberOfChildDFs() + ", child EFs: " + selectedFile.getNumberOfChildEFs()));
                    }
                    break;
                case SimCardFile.EF:
                    switch ((byte) selectResponse.getEFType()) { // fileStructure byte
                        case SimCardElementaryFile.EF_TRANSPARENT:
                            selectedFile = new SimCardTransparentFile(selectResponse);
                            if (DEBUG) {
                                System.out.println(LoggingUtils.formatDebugMessage("selected EF Transparent " + selectResponse.getFileId() + ", size: " + selectedFile.getFileSize()));
                            }
                            break;
                        case SimCardElementaryFile.EF_LINEAR_FIXED:
                            selectedFile = new SimCardLinearFixedFile(selectResponse);
                            if (DEBUG) {
                                System.out.println(LoggingUtils.formatDebugMessage("selected EF Linear-Fixed " + selectResponse.getFileId() + ", size: " + selectedFile.getFileSize()));
                            }
                            break;
                        case SimCardElementaryFile.EF_CYCLIC:
                            selectedFile = new SimCardCyclicFile(selectResponse);
                            if (DEBUG) {
                                System.out.println(LoggingUtils.formatDebugMessage("selected EF Cyclic " + selectResponse.getFileId() + ", size: " + selectedFile.getFileSize()));
                            }
                            break;
                        default:
                            throw new CardException("Unknown EF type while selecting " + HexToolkit.toString(fileId));
                    }
                    break;
                default:
                    throw new CardException("Unknown file type while selecting " + HexToolkit.toString(fileId));

            }
        }

        return selectedFile;
    }

    public static ResponseAPDU selectFileById(byte[] fileId) throws CardException {
        if (DEBUG) {
            System.out.println(LoggingUtils.formatDebugMessage("selecting file: " + HexToolkit.toString(fileId)));
        }

        CommandAPDU select;
        if (SIMLibrary.third_gen_apdu) {
            select = new CommandAPDU((byte) 0x00, (byte) 0xA4, (byte) 0x00, (byte) 0x04, fileId);
        } else {
            select = new CommandAPDU((byte) 0xA0, (byte) 0xA4, (byte) 0x00, (byte) 0x00, fileId);
        }

        ResponseAPDU response = ChannelHandler.transmitOnDefaultChannel(select);

        return response;
    }

    private static SelectResponse2G getResponse2G(byte[] fileId, int bytes) throws CardException {
        ResponseAPDU response = APDUToolkit.getResponse(bytes);

        SelectResponse2G fileData = null;

        if ((short) 0x9000 == (short) response.getSW()) {
            if (DEBUG) {
                System.out.println(LoggingUtils.formatDebugMessage("file " + HexToolkit.toString(fileId) + " selected; "));
            }
            fileData = new SelectResponse2G(response.getData(), HexToolkit.toString(fileId));

        } else {
            System.err.println(LoggingUtils.formatDebugMessage("weird SW received: " + Integer.toHexString(response.getSW1()) + " " + Integer.toHexString(response.getSW2())));
            System.err.println(LoggingUtils.formatDebugMessage("this may mean your reader is not responding well, reconnect it, restart pcscd, reload osmocom BB firmware, etc."));
            System.out.println(LoggingUtils.formatDebugMessage("trying to parse Select command response anyway, this may get ugly.."));
            fileData = new SelectResponse2G(response.getData(), HexToolkit.toString(fileId));
        }

        return fileData;
    }
}
