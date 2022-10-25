package de.srlabs.simlib;

import java.io.FileNotFoundException;
import javax.smartcardio.CardException;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

public class FileManagement {

    private final static boolean LOCAL_DEBUG = false;
    private final static boolean DEBUG = Debug.DEBUG || LOCAL_DEBUG;

    private static SimCardFile getSimCardFileType(String filePathId, SelectResponse selectResponse) throws CardException {
        SimCardFile selectedFile = null;

        switch (selectResponse.getFileType()) { // fileType byte
            case SimCardFile.MF:
                selectedFile = new SimCardMasterFile(selectResponse);
                if (DEBUG) {
                    System.out.println(LoggingUtils.formatDebugMessage("selected MF " + selectResponse.getFileId() + ", child DFs: " + selectedFile.getNumberOfChildDFs() + ", child EFs: " + selectedFile.getNumberOfChildEFs()));
                }
                break;
            case SimCardFile.DF:
            case SimCardFile.ADF:
                selectedFile = new SimCardDirectoryFile(selectResponse);
                if (DEBUG) {
                    System.out.println(LoggingUtils.formatDebugMessage("selected DF " + selectResponse.getFileId() + ", child DFs: " + selectedFile.getNumberOfChildDFs() + ", child EFs: " + selectedFile.getNumberOfChildEFs()));
                }
                break;
            case SimCardFile.EF:
            case SimCardFile.INTERNAL_EF:
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
                    case SimCardElementaryFile.EF_NO_INFO:
                        selectedFile = new SimCardNoInfoFile(selectResponse);
                        if (DEBUG) {
                            System.out.println(LoggingUtils.formatDebugMessage("selected EF NoInfo " + selectResponse.getFileId() + ", size: " + selectedFile.getFileSize()));
                        }
                        break;
                    case SimCardElementaryFile.EF_BER_TLV:
                        selectedFile = new SimCardBerTlvFile(selectResponse);
                        if (DEBUG) {
                            System.out.println(LoggingUtils.formatDebugMessage("selected EF NoInfo " + selectResponse.getFileId() + ", size: " + selectedFile.getFileSize()));
                        }
                        break;
                    default:
                        throw new CardException("Unknown EF type while selecting " + filePathId);
                }
                break;
            default:
                throw new CardException("Unknown file type while selecting " + filePathId + "; File type value: " + selectResponse.getFileType());
        }

        return selectedFile;
    }

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

            selectedFile = getSimCardFileType(HexToolkit.toString(fileId), selectResponse);
        }

        return selectedFile;
    }

    public static byte[] selectAID(byte[] aid) throws CardException, FileNotFoundException {
        if (DEBUG) {
            System.out.println(LoggingUtils.formatDebugMessage("selecting AID: " + HexToolkit.toString(aid)));
        }

        CommandAPDU select = new CommandAPDU((byte) 0x00, (byte) 0xA4, (byte) 0x04, (byte) 0x04, aid);
        ResponseAPDU r = ChannelHandler.transmitOnDefaultChannel(select);

        if ((short) r.getSW() != (short) 0x9000) {
            System.out.println("Application cannot be selected");
            throw new FileNotFoundException("AID: " + HexToolkit.toString(aid) + "; doesn't seem to exist on this card; SW = " + Integer.toHexString(r.getSW()));
        }

        byte[] fid_data = TLVToolkit.getTLV(r.getData(), (byte) 0x83, (byte) 0x82);
        if (fid_data == null) {
            return null;
        }

        return new byte[]{fid_data[2], fid_data[3]};
    }

    /**
     * This function gets a file path as argument and selects the file using 3G APDU.
     * It uses P1=08 which means "Select by path from MF"
     *
     * @param filePath: the path of the file (without slashes)
     * @return SimCardFile
     * @throws CardException
     * @throws FileNotFoundException
     */
    public static SimCardFile selectFileByPath(String filePath) throws CardException, FileNotFoundException {
        // Check if the card accepts 3G APDU
        if (!SIMLibrary.third_gen_apdu) {
            return null;
        }

        CommandAPDU select = new CommandAPDU((byte) 0x00, (byte) 0xA4, (byte) 0x08, (byte) 0x04, HexToolkit.fromString(filePath));
        ResponseAPDU r = ChannelHandler.transmitOnDefaultChannel(select);

        SimCardFile selectedFile;
        SelectResponse selectResponse;
        if ((short) 0x9000 == (short) r.getSW()) {
            selectResponse = new SelectResponse3G(r.getData());
        } else if ((short) 0x6A82 == (short) r.getSW()) {
            throw new FileNotFoundException("file ID: " + filePath + "; doesn't seem to exist on this card; SW = " + Integer.toHexString(r.getSW()));
        } else {
            throw new CardException("an unexpected error has occurred during selection of file ID: " + filePath + "; SW = " + Integer.toHexString(r.getSW()) + "; APDU = " + HexToolkit.toString(select.getBytes()));
        }

        selectedFile = getSimCardFileType(filePath, selectResponse);

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

        return ChannelHandler.transmitOnDefaultChannel(select);
    }

    private static SelectResponse2G getResponse2G(byte[] fileId, int bytes) throws CardException {
        ResponseAPDU response = APDUToolkit.getResponse(bytes);

        SelectResponse2G fileData = null;

        // TODO: why is this if statement here if the fileData gets the same value for both cases?
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
