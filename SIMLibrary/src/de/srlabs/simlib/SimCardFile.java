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
    public final static Map<String, String[]> _fileMap;

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

    static {
        Map<String, String[]> aMap = new HashMap<String, String[]>();
        aMap.put("3f00", new String[]{"MF", "Root Directory"});
        aMap.put("3f00/2f00", new String[]{"EFdir", "Application directory"});
        aMap.put("3f00/2f05", new String[]{"PL", "Preferred Languages"});
        aMap.put("3f00/2f06", new String[]{"MF/arr", "Access Rule Reference"});
        aMap.put("3f00/2f53", new String[]{"Axalto (U)Simera", "file that is used by the STK framework in case of an SMS PP Unformatted UPD"}); //FIXME: this is crap from some forum
        aMap.put("3f00/2fe2", new String[]{"ICCID", "ICC Identification"});
        aMap.put("3f00/5f11", new String[]{"Gemplus GemXpresso", "file that is used by the STK framework in case of an SMS PP Unformatted UPD"}); //FIXME: this is crap from some forum
        aMap.put("3f00/7f10", new String[]{"DFtelecom", "Telecom Directory"});
        aMap.put("3f00/7f10/6f06", new String[]{"DFtelecom/arr", "Access Rule Reference"});
        aMap.put("3f00/7f10/6f3a", new String[]{"ADN", "Abbrev numbers"});
        aMap.put("3f00/7f10/6f3b", new String[]{"FDN", "Fixed numbers"});
        aMap.put("3f00/7f10/6f3c", new String[]{"SMS", "SMS"});
        aMap.put("3f00/7f10/6f3d", new String[]{"CCP", "Capability conf"});
        aMap.put("3f00/7f10/6f40", new String[]{"MSISDN", "MSISDN"});
        aMap.put("3f00/7f10/6f42", new String[]{"SMSP", "SMS parameters"});
        aMap.put("3f00/7f10/6f43", new String[]{"SMSS", "SMS status"});
        aMap.put("3f00/7f10/6f44", new String[]{"LND", "Last number dialled"});
        aMap.put("3f00/7f10/6f47", new String[]{"SMSR", "SMS Status reports"});
        aMap.put("3f00/7f10/6f49", new String[]{"SDN", "Service Dialling Nrs"});
        aMap.put("3f00/7f10/6f4a", new String[]{"EXT1", "Extension 1"});
        aMap.put("3f00/7f10/6f4b", new String[]{"EXT2", "Extension 2"});
        aMap.put("3f00/7f10/6f4c", new String[]{"EXT3", "Extension 3"});
        aMap.put("3f00/7f10/6f4d", new String[]{"BDN", "Barred Numbers"});
        aMap.put("3f00/7f10/6f4e", new String[]{"EXT4", "Extension 4"});
        aMap.put("3f00/7f10/6f4f", new String[]{"ECCP", "Extended Capabilities"});
        aMap.put("3f00/7f10/6f58", new String[]{"CMI", "Comparision Method"});
        aMap.put("3f00/7f10/5f3a", new String[]{"DFphonebook", "Phone book Directory"});
        aMap.put("3f00/7f10/5f3a/4f22", new String[]{"PSC", "PBook Sync counter"});
        aMap.put("3f00/7f10/5f3a/4f23", new String[]{"CC", "PBook Change counter"});
        aMap.put("3f00/7f10/5f3a/4f24", new String[]{"PUID", "PBook previous UID"});
        aMap.put("3f00/7f10/5f3a/4f30", new String[]{"PBR", "PBook reference"});
        aMap.put("3f00/7f10/5f3a/0000", new String[]{"PBccp1", "PBook Capability"});
        aMap.put("3f00/7f10/5f3a/4f09", new String[]{"PBc", "PBook Control"});
        aMap.put("3f00/7f10/5f3a/4f11", new String[]{"PBanr", "PBook addition nr"});
        aMap.put("3f00/7f10/5f3a/4f19", new String[]{"PBsne", "PBook Second Name"});
        aMap.put("3f00/7f10/5f3a/4f21", new String[]{"PBuid", "PBook Unique ID"});
        aMap.put("3f00/7f10/5f3a/4f26", new String[]{"PBgrp", "PBook groups"});
        aMap.put("3f00/7f10/5f3a/4f3a", new String[]{"PBadn", "PBook Abbrev numbers"});
        aMap.put("3f00/7f10/5f3a/4f4a", new String[]{"PBext1", "PBook extension 1"});
        aMap.put("3f00/7f10/5f3a/4f4b", new String[]{"PBass", "PBook additional nrs"});
        aMap.put("3f00/7f10/5f3a/4f4c", new String[]{"PBgas", "PBook group desc"});
        aMap.put("3f00/7f10/5f3a/4f50", new String[]{"PBemail", "PBook email address"});
        aMap.put("3f00/7f10/5f50/4f20", new String[]{"IMG", "Image"});
        aMap.put("3f00/7f20", new String[]{"DFgsm", "GSM Directory"});
        aMap.put("3f00/7f20/6f05", new String[]{"LP", "Language Preference"});
        aMap.put("3f00/7f20/6f07", new String[]{"IMSI", "IMSI"});
        aMap.put("3f00/7f20/6f09", new String[]{"KeyPS", "Keys for Packet domain"});
        aMap.put("3f00/7f20/6f20", new String[]{"Kc", "Ciphering key Kc"});
        aMap.put("3f00/7f20/6f2c", new String[]{"DCK", "De-personalise Key"});
        aMap.put("3f00/7f20/6f30", new String[]{"PLMNsel", "PLMN selector"});
        aMap.put("3f00/7f20/6f31", new String[]{"HPPLMN", "HPLMN search period"});
        aMap.put("3f00/7f20/6f32", new String[]{"CNL", "Co-operative Networks"});
        aMap.put("3f00/7f20/6f37", new String[]{"ACMmax", "ACM maximum value"});
        aMap.put("3f00/7f20/6f38", new String[]{"SST", "SIM service table"});
        aMap.put("3f00/7f20/6f39", new String[]{"ACM", "Call meter"});
        aMap.put("3f00/7f20/6f3e", new String[]{"GID1", "Group Id Level 1"});
        aMap.put("3f00/7f20/6f3f", new String[]{"GID2", "Group Id Level 2"});
        aMap.put("3f00/7f20/6f41", new String[]{"PUCT", "Price per unit"});
        aMap.put("3f00/7f20/6f45", new String[]{"CBMI", "Bcast msg id"});
        aMap.put("3f00/7f20/6f46", new String[]{"SPN", "Network Name"});
        aMap.put("3f00/7f20/6f48", new String[]{"CMMID", "Cell BCast for Data"});
        aMap.put("3f00/7f20/6f50", new String[]{"CBMIR", "Cell BCast msg ID"});
        aMap.put("3f00/7f20/6f51", new String[]{"NIA", "Network Ind Alerting"});
        aMap.put("3f00/7f20/6f52", new String[]{"KcGPRS", "GPRS Ciphering Key"});
        aMap.put("3f00/7f20/6f53", new String[]{"LOCIGPRS", "GPRS location info"});
        aMap.put("3f00/7f20/6f54", new String[]{"SUME", "SetUpMenu Elements"});
        aMap.put("3f00/7f20/6f60", new String[]{"PLMNwAcT", "User PLMN Selector"});
        aMap.put("3f00/7f20/6f61", new String[]{"OPLMNwAcT", "OPerator PLMN Select"});
        aMap.put("3f00/7f20/6f62", new String[]{"HPLMNwAcT", "HPLMN Selector"});
        aMap.put("3f00/7f20/6f63", new String[]{"CPBCCH", "CPBCCH Information"});
        aMap.put("3f00/7f20/6f64", new String[]{"InvScan", "Investingation Scan"});
        aMap.put("3f00/7f20/6f74", new String[]{"BCCH", "Bcast control chans"});
        aMap.put("3f00/7f20/6f78", new String[]{"ACC", "Access control class"});
        aMap.put("3f00/7f20/6f7b", new String[]{"FPLMN", "Forbidden PLMNs"});
        aMap.put("3f00/7f20/6f7e", new String[]{"LOCI", "Location information"});
        aMap.put("3f00/7f20/6fad", new String[]{"AD", "Administrative data"});
        aMap.put("3f00/7f20/6fae", new String[]{"Phase", "Phase identification"});
        aMap.put("3f00/7f20/6fb1", new String[]{"VGCS", "Voice call group srv"});
        aMap.put("3f00/7f20/6fb2", new String[]{"VGCSS", "Voice group call stat"});
        aMap.put("3f00/7f20/6fb3", new String[]{"VBS", "Voice broadcast srv"});
        aMap.put("3f00/7f20/6fb4", new String[]{"VBSS", "Voice broadcast stat"});
        aMap.put("3f00/7f20/6fb5", new String[]{"eMLPP", "Enhan Mult Lvl Prio"});
        aMap.put("3f00/7f20/6fb6", new String[]{"AAeM", "Auto Answer for eMLPP"});
        aMap.put("3f00/7f20/6fb7", new String[]{"ECC", "Emergency Call Codes"});
        aMap.put("3f00/7f20/6fc5", new String[]{"PNN", "PLMN Network Name"});
        aMap.put("3f00/7f20/6fc6", new String[]{"OPL", "Operator PLMN list"});
        aMap.put("3f00/7f20/6fc7", new String[]{"MBDN", "Mailbox Dialling Nrs"});
        aMap.put("3f00/7f20/6fc8", new String[]{"EXT6", "Extension6"});
        aMap.put("3f00/7f20/6fc9", new String[]{"MBI", "Mailbox Identifier"});
        aMap.put("3f00/7f20/6fca", new String[]{"MWIS", "Message Waiting Stat"});
        aMap.put("3f00/7f20/6fcb", new String[]{"CFIS", "Call Fwd Ind Status"});
        aMap.put("3f00/7f20/6fcc", new String[]{"EXT7", "Extension7"});
        aMap.put("3f00/7f20/6fcd", new String[]{"SPDI", "Service Prov Display"});
        aMap.put("3f00/7f20/6fce", new String[]{"MMSN", "MMS Notification"});
        aMap.put("3f00/7f20/6fcf", new String[]{"EXT8", "Extension8"});
        aMap.put("3f00/7f20/6fd0", new String[]{"MMSICP", "MMS Issuer Params"});
        aMap.put("3f00/7f20/6fd1", new String[]{"MMSUP", "MMS User Preferences"});
        aMap.put("3f00/7f20/6fd2", new String[]{"MMSUCP", "MMS User Params"});
        aMap.put("3f00/7f20/6f06", new String[]{"USIMarr", "Access Rule Reference"});
        aMap.put("3f00/7f20/6f08", new String[]{"USIMkeys", "Ciphering and keys"});
        aMap.put("3f00/7f20/6f3b", new String[]{"USIMfdn", "Fixed numbers"});
        aMap.put("3f00/7f20/6f3c", new String[]{"USIMsms", "SMS"});
        aMap.put("3f00/7f20/6f40", new String[]{"USIMmsisdn", "MSISDN"});
        aMap.put("3f00/7f20/6f42", new String[]{"USIMsmsp", "SMS parameters"});
        aMap.put("3f00/7f20/6f43", new String[]{"USIMsmss", "SMS status"});
        aMap.put("3f00/7f20/6f47", new String[]{"USIMsmsr", "SMS Status reports"});
        aMap.put("3f00/7f20/6f49", new String[]{"USIMsdn", "Service Dialling Nrs"});
        aMap.put("3f00/7f20/6f4a", new String[]{"USIMext1", "Extension 1"});
        aMap.put("3f00/7f20/6f4b", new String[]{"USIMext2", "Extension 2"});
        aMap.put("3f00/7f20/6f4c", new String[]{"USIMext3", "Extension 3"});
        aMap.put("3f00/7f20/6f4d", new String[]{"USIMbdn", "Barred Numbers"});
        aMap.put("3f00/7f20/6f4e", new String[]{"USIMext5", "Extension 5"});
        aMap.put("3f00/7f20/6f4f", new String[]{"USIMccp2", "Capability Config 2"});
        aMap.put("3f00/7f20/6f55", new String[]{"USIMext4", "Extension 4"});
        aMap.put("3f00/7f20/6f56", new String[]{"USIMest", "Enabled Services"});
        aMap.put("3f00/7f20/6f57", new String[]{"USIMacl", "Access Control List"});
        aMap.put("3f00/7f20/6f58", new String[]{"USIMcmi", "Comparision Method"});
        aMap.put("3f00/7f20/6f5b", new String[]{"USIMstarthfn", "Init for Hyperframe"});
        aMap.put("3f00/7f20/6f5c", new String[]{"USIMthreshold", "Max value for START"});
        aMap.put("3f00/7f20/6f73", new String[]{"USIMpsloci", "Packet switched locn"});
        aMap.put("3f00/7f20/6f80", new String[]{"USIMici", "Incoming Calls"});
        aMap.put("3f00/7f20/6f81", new String[]{"USIMoci", "Outgoing Calls"});
        aMap.put("3f00/7f20/6f82", new String[]{"USIMict", "Incoming Call Timer"});
        aMap.put("3f00/7f20/6f83", new String[]{"USIMoct", "Outgoing Call Timer"});
        aMap.put("3f00/7f20/6fc3", new String[]{"USIMhiddenkey", "Key for hidden pbook"});
        aMap.put("3f00/7f20/6fc4", new String[]{"USIMnetpar", "Network parameters"});
        aMap.put("3f00/7f20/6fd3", new String[]{"USIMnia", "Network Ind Alerting"});
        aMap.put("3f00/7f20/6fd4", new String[]{"USIMvgcsca", "Voice grp cipher alg"});
        aMap.put("3f00/7f20/6fd5", new String[]{"USIMvbsca", "Voice brd cipher alg"});
        aMap.put("3f00/7f20/6fd6", new String[]{"USIMgbabp", "GBA Bootstrap"});
        aMap.put("3f00/7f20/6fd7", new String[]{"USIMmsk", "MBMS Service Keys"});
        aMap.put("3f00/7f20/6fd8", new String[]{"USIMmuk", "MBMS User Key"});
        aMap.put("3f00/7f20/6fd9", new String[]{"USIMehplmn", "Evuiv HPLMN"});
        aMap.put("3f00/7f20/6fda", new String[]{"USIMgbanl", "GBA NAF List"});
        aMap.put("3f00/7f20/5f3b", new String[]{"DFmultimedia", "Multi media Directory"});
        aMap.put("3f00/7f21", new String[]{"DF DCS1800", "DCS1800 Directory"});
        aMap.put("3f00/7f21/6f05", new String[]{"LP", "Language Preference"});
        aMap.put("3f00/7f21/6f07", new String[]{"IMSI", "IMSI"});
        aMap.put("3f00/7f21/6f09", new String[]{"KeyPS", "Keys for Packet domain"});
        aMap.put("3f00/7f21/6f20", new String[]{"Kc", "Ciphering key Kc"});
        aMap.put("3f00/7f21/6f2c", new String[]{"DCK", "De-personalise Key"});
        aMap.put("3f00/7f21/6f30", new String[]{"PLMNsel", "PLMN selector"});
        aMap.put("3f00/7f21/6f31", new String[]{"HPPLMN", "HPLMN search period"});
        aMap.put("3f00/7f21/6f32", new String[]{"CNL", "Co-operative Networks"});
        aMap.put("3f00/7f21/6f37", new String[]{"ACMmax", "ACM maximum value"});
        aMap.put("3f00/7f21/6f38", new String[]{"SST", "SIM service table"});
        aMap.put("3f00/7f21/6f39", new String[]{"ACM", "Call meter"});
        aMap.put("3f00/7f21/6f3e", new String[]{"GID1", "Group Id Level 1"});
        aMap.put("3f00/7f21/6f3f", new String[]{"GID2", "Group Id Level 2"});
        aMap.put("3f00/7f21/6f41", new String[]{"PUCT", "Price per unit"});
        aMap.put("3f00/7f21/6f45", new String[]{"CBMI", "Bcast msg id"});
        aMap.put("3f00/7f21/6f46", new String[]{"SPN", "Network Name"});
        aMap.put("3f00/7f21/6f48", new String[]{"CMMID", "Cell BCast for Data"});
        aMap.put("3f00/7f21/6f50", new String[]{"CBMIR", "Cell BCast msg ID"});
        aMap.put("3f00/7f21/6f51", new String[]{"NIA", "Network Ind Alerting"});
        aMap.put("3f00/7f21/6f52", new String[]{"KcGPRS", "GPRS Ciphering Key"});
        aMap.put("3f00/7f21/6f53", new String[]{"LOCIGPRS", "GPRS location info"});
        aMap.put("3f00/7f21/6f54", new String[]{"SUME", "SetUpMenu Elements"});
        aMap.put("3f00/7f21/6f60", new String[]{"PLMNwAcT", "User PLMN Selector"});
        aMap.put("3f00/7f21/6f61", new String[]{"OPLMNwAcT", "OPerator PLMN Select"});
        aMap.put("3f00/7f21/6f62", new String[]{"HPLMNwAcT", "HPLMN Selector"});
        aMap.put("3f00/7f21/6f63", new String[]{"CPBCCH", "CPBCCH Information"});
        aMap.put("3f00/7f21/6f64", new String[]{"InvScan", "Investingation Scan"});
        aMap.put("3f00/7f21/6f74", new String[]{"BCCH", "Bcast control chans"});
        aMap.put("3f00/7f21/6f78", new String[]{"ACC", "Access control class"});
        aMap.put("3f00/7f21/6f7b", new String[]{"FPLMN", "Forbidden PLMNs"});
        aMap.put("3f00/7f21/6f7e", new String[]{"LOCI", "Location information"});
        aMap.put("3f00/7f21/6fad", new String[]{"AD", "Administrative data"});
        aMap.put("3f00/7f21/6fae", new String[]{"Phase", "Phase identification"});
        aMap.put("3f00/7f21/6fb1", new String[]{"VGCS", "Voice call group srv"});
        aMap.put("3f00/7f21/6fb2", new String[]{"VGCSS", "Voice group call stat"});
        aMap.put("3f00/7f21/6fb3", new String[]{"VBS", "Voice broadcast srv"});
        aMap.put("3f00/7f21/6fb4", new String[]{"VBSS", "Voice broadcast stat"});
        aMap.put("3f00/7f21/6fb5", new String[]{"eMLPP", "Enhan Mult Lvl Prio"});
        aMap.put("3f00/7f21/6fb6", new String[]{"AAeM", "Auto Answer for eMLPP"});
        aMap.put("3f00/7f21/6fb7", new String[]{"ECC", "Emergency Call Codes"});
        aMap.put("3f00/7f21/6fc5", new String[]{"PNN", "PLMN Network Name"});
        aMap.put("3f00/7f21/6fc6", new String[]{"OPL", "Operator PLMN list"});
        aMap.put("3f00/7f21/6fc7", new String[]{"MBDN", "Mailbox Dialling Nrs"});
        aMap.put("3f00/7f21/6fc8", new String[]{"EXT6", "Extension6"});
        aMap.put("3f00/7f21/6fc9", new String[]{"MBI", "Mailbox Identifier"});
        aMap.put("3f00/7f21/6fca", new String[]{"MWIS", "Message Waiting Stat"});
        aMap.put("3f00/7f21/6fcb", new String[]{"CFIS", "Call Fwd Ind Status"});
        aMap.put("3f00/7f21/6fcc", new String[]{"EXT7", "Extension7"});
        aMap.put("3f00/7f21/6fcd", new String[]{"SPDI", "Service Prov Display"});
        aMap.put("3f00/7f21/6fce", new String[]{"MMSN", "MMS Notification"});
        aMap.put("3f00/7f21/6fcf", new String[]{"EXT8", "Extension8"});
        aMap.put("3f00/7f21/6fd0", new String[]{"MMSICP", "MMS Issuer Params"});
        aMap.put("3f00/7f21/6fd1", new String[]{"MMSUP", "MMS User Preferences"});
        aMap.put("3f00/7f21/6fd2", new String[]{"MMSUCP", "MMS User Params"});
        aMap.put("3f00/7f21/6f06", new String[]{"USIMarr", "Access Rule Reference"});
        aMap.put("3f00/7f21/6f08", new String[]{"USIMkeys", "Ciphering and keys"});
        aMap.put("3f00/7f21/6f3b", new String[]{"USIMfdn", "Fixed numbers"});
        aMap.put("3f00/7f21/6f3c", new String[]{"USIMsms", "SMS"});
        aMap.put("3f00/7f21/6f40", new String[]{"USIMmsisdn", "MSISDN"});
        aMap.put("3f00/7f21/6f42", new String[]{"USIMsmsp", "SMS parameters"});
        aMap.put("3f00/7f21/6f43", new String[]{"USIMsmss", "SMS status"});
        aMap.put("3f00/7f21/6f47", new String[]{"USIMsmsr", "SMS Status reports"});
        aMap.put("3f00/7f21/6f49", new String[]{"USIMsdn", "Service Dialling Nrs"});
        aMap.put("3f00/7f21/6f4a", new String[]{"USIMext1", "Extension 1"});
        aMap.put("3f00/7f21/6f4b", new String[]{"USIMext2", "Extension 2"});
        aMap.put("3f00/7f21/6f4c", new String[]{"USIMext3", "Extension 3"});
        aMap.put("3f00/7f21/6f4d", new String[]{"USIMbdn", "Barred Numbers"});
        aMap.put("3f00/7f21/6f4e", new String[]{"USIMext5", "Extension 5"});
        aMap.put("3f00/7f21/6f4f", new String[]{"USIMccp2", "Capability Config 2"});
        aMap.put("3f00/7f21/6f55", new String[]{"USIMext4", "Extension 4"});
        aMap.put("3f00/7f21/6f56", new String[]{"USIMest", "Enabled Services"});
        aMap.put("3f00/7f21/6f57", new String[]{"USIMacl", "Access Control List"});
        aMap.put("3f00/7f21/6f58", new String[]{"USIMcmi", "Comparision Method"});
        aMap.put("3f00/7f21/6f5b", new String[]{"USIMstarthfn", "Init for Hyperframe"});
        aMap.put("3f00/7f21/6f5c", new String[]{"USIMthreshold", "Max value for START"});
        aMap.put("3f00/7f21/6f73", new String[]{"USIMpsloci", "Packet switched locn"});
        aMap.put("3f00/7f21/6f80", new String[]{"USIMici", "Incoming Calls"});
        aMap.put("3f00/7f21/6f81", new String[]{"USIMoci", "Outgoing Calls"});
        aMap.put("3f00/7f21/6f82", new String[]{"USIMict", "Incoming Call Timer"});
        aMap.put("3f00/7f21/6f83", new String[]{"USIMoct", "Outgoing Call Timer"});
        aMap.put("3f00/7f21/6fc3", new String[]{"USIMhiddenkey", "Key for hidden pbook"});
        aMap.put("3f00/7f21/6fc4", new String[]{"USIMnetpar", "Network parameters"});
        aMap.put("3f00/7f21/6fd3", new String[]{"USIMnia", "Network Ind Alerting"});
        aMap.put("3f00/7f21/6fd4", new String[]{"USIMvgcsca", "Voice grp cipher alg"});
        aMap.put("3f00/7f21/6fd5", new String[]{"USIMvbsca", "Voice brd cipher alg"});
        aMap.put("3f00/7f21/6fd6", new String[]{"USIMgbabp", "GBA Bootstrap"});
        aMap.put("3f00/7f21/6fd7", new String[]{"USIMmsk", "MBMS Service Keys"});
        aMap.put("3f00/7f21/6fd8", new String[]{"USIMmuk", "MBMS User Key"});
        aMap.put("3f00/7f21/6fd9", new String[]{"USIMehplmn", "Evuiv HPLMN"});
        aMap.put("3f00/7f21/6fda", new String[]{"USIMgbanl", "GBA NAF List"});
        aMap.put("3f00/7f21/5f3b", new String[]{"DFmultimedia", "Multi media Directory"});
        aMap.put("3f00/7f45", new String[]{"S@T", "SIMalliance Toolbox"});
        aMap.put("3f00/7f55", new String[]{"N@vigate", ""});
        aMap.put("3f00/7ff0", new String[]{"3G shadow dir", "used to link 3G files to 2G context, kind of wrong"});

        _fileMap = Collections.unmodifiableMap(aMap);
    }
}
