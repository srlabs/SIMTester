package de.srlabs.simlib;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class USIMCardFileMapping extends SimCardFileMapping {

    @Override
    protected Map<String, String[]> getFilesDetails() {
        return new HashMap<String, String[]>() {{
            put("7fff/6f05", new String[]{"LI", "Language indication"});
            put("7fff/6f06", new String[]{"ARR", "Access Rule Reference"});
            put("7fff/6f07", new String[]{"IMSI", "International Mobile Subscriber Identity"});
            put("7fff/6f08", new String[]{"Keys", "Ciphering and Integrity Keys"});
            put("7fff/6f09", new String[]{"KeysPS", "Ciphering and Integrity Keys for Packet Switched domain"});
            put("7fff/6f2C", new String[]{"DCK", "Depersonalisation Control Keys"});
            put("7fff/6f31", new String[]{"HPPLMN", "Higher Priority PLMN search period"});
            put("7fff/6f32", new String[]{"CNL", "Co-operative Network List"});
            put("7fff/6f37", new String[]{"ACMmax", "ACM maximum value"});
            put("7fff/6f38", new String[]{"UST", "USIM Service Table"});
            put("7fff/6f39", new String[]{"ACM", "Accumulated Call Meter"});
            put("7fff/6f3b", new String[]{"FDN", "Fixed Dialling Numbers"});
            put("7fff/6f3c", new String[]{"SMS", "Short messages"});
            put("7fff/6f3e", new String[]{"GID1", "Group Identifier Level 1"});
            put("7fff/6f3f", new String[]{"GID2", "Group Identifier Level 2"});
            put("7fff/6f40", new String[]{"MSISDN", "MSISDN"});
            put("7fff/6f41", new String[]{"PUCT", "Price per Unit and Currency Table"});
            put("7fff/6f42", new String[]{"SMSP", "Short message service parameters"});
            put("7fff/6f43", new String[]{"SMSS", "SMS status"});
            put("7fff/6f45", new String[]{"CBMI", "Cell Broadcast Message identifier selection"});
            put("7fff/6f46", new String[]{"SPN", "Service Provider Name"});
            put("7fff/6f47", new String[]{"SMSR", "Short message status reports"});
            put("7fff/6f48", new String[]{"CBMID", "Cell Broadcast Message Identifier for Data Download"});
            put("7fff/6f49", new String[]{"SDN", "Service Dialling Numbers"});
            put("7fff/6f4b", new String[]{"EXT2", "Extension2"});
            put("7fff/6f4c", new String[]{"EXT3", "Extension3"});
            put("7fff/6f4d", new String[]{"BDN", "Barred Dialling Numbers"});
            put("7fff/6f4e", new String[]{"EXT5", "Extension5"});
            put("7fff/6f4f", new String[]{"6f4f", "Capability Configuration Parameters 2"});
            put("7fff/6f50", new String[]{"CBMIR", "Cell Broadcast Message Identifier Range selection"});
            put("7fff/6f55", new String[]{"EXT4", "Extension4"});
            put("7fff/6f56", new String[]{"EST", "Enabled Services Table"});
            put("7fff/6f57", new String[]{"ACL", "Access Point Name Control List"});
            put("7fff/6f58", new String[]{"CMI", "Comparison Method Information"});
            put("7fff/6f5b", new String[]{"START-HFN", "Initialisation values for Hyperframe number"});
            put("7fff/6f5c", new String[]{"THRESHOLD", "Maximum value of START"});
            put("7fff/6f60", new String[]{"PLMNwAcT", "User controlled PLMN selector with Access Technology"});
            put("7fff/6f61", new String[]{"OPLMNwACT", "Operator controlled PLMN selector with Access Technology"});
            put("7fff/6f62", new String[]{"HPLMNwAcT", "HPLMN selector with Access Technology"});
            put("7fff/6f73", new String[]{"PSLOCI", "Packet Switched location information"});
            put("7fff/6f78", new String[]{"ACC", "Access Control Class"});
            put("7fff/6f7b", new String[]{"FPLMN", "Forbidden PLMNs"});
            put("7fff/6f7e", new String[]{"LOCI", "Location Information"});
            put("7fff/6f80", new String[]{"ICI", "Incoming Call Information"});
            put("7fff/6f81", new String[]{"OCI", "Outgoing Call Information"});
            put("7fff/6f82", new String[]{"ICT", "Incoming Call Timer"});
            put("7fff/6f83", new String[]{"OCT", "Outgoing Call Timer"});
            put("7fff/6fad", new String[]{"AD", "Administrative Data"});
            put("7fff/6fb1", new String[]{"VGCS", "Voice Group Call Service"});
            put("7fff/6fb2", new String[]{"VGCSS", "Voice Group Call Service Status"});
            put("7fff/6fb3", new String[]{"VBS", "Voice Broadcast Service"});
            put("7fff/6fb4", new String[]{"VBSS", "Voice Broadcast Service Status"});
            put("7fff/6fb5", new String[]{"eMLPP", "enhanced Multi Level Precedence and Pre-emption"});
            put("7fff/6fb6", new String[]{"AaeM", "Automatic Answer for eMLPP Service"});
            put("7fff/6fb7", new String[]{"ECC", "Emergency Call Codes"});
            put("7fff/6fc3", new String[]{"Hiddenkey", "Key for hidden phone book entries"});
            put("7fff/6fc4", new String[]{"NETPAR", "Network Parameters"});
            put("7fff/6fc5", new String[]{"PNN", "PLMN Network Name"});
            put("7fff/6fc6", new String[]{"OPL", "Operator PLMN List"});
            put("7fff/6fc7", new String[]{"MBDN", "Mailbox Dialling Numbers"});
            put("7fff/6fc8", new String[]{"EXT6", "Extension6"});
            put("7fff/6fc9", new String[]{"MBI", "Mailbox Identifier"});
            put("7fff/6fca", new String[]{"MWIS", "Message Waiting Indication Status"});
            put("7fff/6fcb", new String[]{"CFIS", "Call Forwarding Indication Status"});
            put("7fff/6fcc", new String[]{"EXT7", "Extension7"});
            put("7fff/6fcd", new String[]{"SPDI", "Service Provider Display Information"});
            put("7fff/6fce", new String[]{"MMSN", "MMS Notification"});
            put("7fff/6fcf", new String[]{"EXT8", "Extension8"});
            put("7fff/6fd0", new String[]{"MMSICP", "MMS Issuer Connectivity Parameters"});
            put("7fff/6fd1", new String[]{"MMSUP", "MMS User Preferences"});
            put("7fff/6fd2", new String[]{"MMSUCP", "MMS User Connectivity Parameters"});
            put("7fff/6fd3", new String[]{"NIA", "Network's Indication of Alerting"});
            put("7fff/6fd4", new String[]{"VGCSCA", "Voice Group Call Service Ciphering Algorithm"});
            put("7fff/6fd5", new String[]{"VBSCA", "Voice Broadcast Service Ciphering Algorithm"});
            put("7fff/6fd6", new String[]{"GBABP", "GBA Bootstrapping parameters"});
            put("7fff/6fd7", new String[]{"MSK", "MBMS Service Keys List"});
            put("7fff/6fd8", new String[]{"MUK", "MBMS User Key"});
            put("7fff/6fd9", new String[]{"EHPLMN", "Equivalent HPLMN"});
            put("7fff/6fda", new String[]{"GBANL", "GBA NAF List"});
            put("7fff/6fdb", new String[]{"EHPLMNPI", "Equivalent HPLMN Presentation Indication"});
            put("7fff/6fdc", new String[]{"LRPLMNSI", "Last RPLMN Selection Indication"});
            put("7fff/6fdd", new String[]{"NAFKCA", "NAF Key Centre Address"});
            put("7fff/6fde", new String[]{"SPNI", "Service Provider Name Icon"});
            put("7fff/6fdf", new String[]{"PNNI", "PLMN Network Name Icon"});
            put("7fff/6fe2", new String[]{"NCP-IP", "Network Connectivity Parameters for USIM IP connections"});
            put("7fff/6fe3", new String[]{"EPSLOCI", "EPS location information"});
            put("7fff/6fe4", new String[]{"EPSNSC", "EPS NAS Security Context"});
            put("7fff/6fe6", new String[]{"UFC", "USAT Facility Control"});
            put("7fff/6fe7", new String[]{"UICCIARI", "UICC IARI"});
            put("7fff/6fe8", new String[]{"NASCONFIG", "Non Access Stratum Configuration"});
            put("7fff/6fec", new String[]{"PWS", "Public Warning System"});
            put("7fff/6fed", new String[]{"FDNURI", "Fixed Dialling Numbers URI"});
            put("7fff/6fee", new String[]{"BDNURI", "Barred Dialling Numbers URI"});
            put("7fff/6fef", new String[]{"SDNURI", "Service Dialling Numbers URI"});
            put("7fff/6ff0", new String[]{"IWL", "IMEI(SV) White Lists"});
            put("7fff/6ff1", new String[]{"IPS", "IMEI(SV) Pairing Status"});
            put("7fff/6ff2", new String[]{"IPD", "IMEI(SV) of Pairing Device"});
            put("7fff/6ff3", new String[]{"ePDGId", "Home ePDG Identifier"});
            put("7fff/6ff4", new String[]{"ePDGSelection", "ePDG Selection Information"});
            put("7fff/6ff5", new String[]{"ePDGIdEm", "Emergency ePDG Identifier"});
            put("7fff/6ff6", new String[]{"ePDGSelectionEm", "ePDG Selection Information for Emergency Services"});
            put("7fff/6ff7", new String[]{"FromPreferred", "From Preferred"});
            put("7fff/6ff8", new String[]{"IMSConfigData", "IMS Configuration Data"});
            put("7fff/6ff9", new String[]{"3GPPPSDATAOFF", "3GPP PS Data Off"});
            put("7fff/6ffa", new String[]{"3GPPPSDATAOFFservicelist", "3GPP PS Data Off Service List"});
            put("7fff/6ffb", new String[]{"TVCONFIG", "TV Configuration"});
            put("7fff/6ffc", new String[]{"XCAPConfigData", "XCAP Configuration Data"});
            put("7fff/6ffd", new String[]{"EARFCNList", "EARFCN list for MTC/NB-IOT UEs"});
            put("7fff/6ffe", new String[]{"MuDMiDConfigData", "MuD and MiD configuration data"});

            put("7fff/5f3a", new String[]{"DFphonebook", "Phone book Directory"});
            put("7fff/5f3a/4f22", new String[]{"PSC", "Phone book Synchronisation Counter"});
            put("7fff/5f3a/4f23", new String[]{"CC", "Change Counter"});
            put("7fff/5f3a/4f24", new String[]{"PUID", "Previous Unique Identifier"});
            // FIXME: https://www.etsi.org/deliver/etsi_ts/131100_131199/131102/16.04.00_60/ts_131102v160400p.pdf page 231
            // specifies more files under DF 5f3a, but they have the format of 4FXX. find out what that XX is and implement it...

            put("7fff/5f3b", new String[]{"DFgsm-access", "GSM Access Directory"});
            put("7fff/5f3b/4f20", new String[]{"Kc", "GSM Ciphering key Kc"});
            put("7fff/5f3b/4f52", new String[]{"KcGPRS", "GPRS Ciphering key KcGPRS"});
            put("7fff/5f3b/4f63", new String[]{"CPBCCH", "CPBCCH Information"});
            put("7fff/5f3b/4f64", new String[]{"InvScan", "Investigation Scan"});

            put("7fff/5f3c", new String[]{"DFmexe", "Mobile Execution Environment Directory"});
            put("7fff/5f3c/4f40", new String[]{"MexE-ST", "MexE Service table"});
            put("7fff/5f3c/4f41", new String[]{"ORPK", "Operator Root Public Key"});
            put("7fff/5f3c/4f42", new String[]{"ARPK", "Administrator Root Public Key"});
            put("7fff/5f3c/4f43", new String[]{"TPRPK", "Third Party Root Public Key"});
            // FIXME: https://www.etsi.org/deliver/etsi_ts/131100_131199/131102/16.04.00_60/ts_131102v160400p.pdf page 231
            // specifies more files under DF 5f3a, but they have the format of 4FXX. find out what that XX is and implement it...

            put("7fff/5f70", new String[]{"DFsolsa", "Support of Localised Service Area Directory"});
            put("7fff/5f70/4f30", new String[]{"SAI", "SoLSA Access Indicator"});
            put("7fff/5f70/4f31", new String[]{"SLL", "SoLSA LSA List"});

            put("7fff/5f40", new String[]{"DFwlan", "WLAN Directory"});
            put("7fff/5f40/4f41", new String[]{"Pseudo", "Pseudonym"});
            put("7fff/5f40/4f42", new String[]{"UPLMNWLAN", "User controlled PLMN selector for I-WLAN Access"});
            put("7fff/5f40/4f43", new String[]{"TPRPK", "Third Party Root Public Key"});
            put("7fff/5f40/4f44", new String[]{"UWSIDL", "User controlled WLAN Specific Identifier List"});
            put("7fff/5f40/4f45", new String[]{"OWSIDL", "Operator controlled WLAN Specific IdentifierList"});
            put("7fff/5f40/4f46", new String[]{"WRI", "WLAN Reauthentication Identity"});
            put("7fff/5f40/4f47", new String[]{"HWSIDL", "Home I-WLAN Specific Identifier List"});
            put("7fff/5f40/4f48", new String[]{"WEHPLMNPI", "I-WLAN Equivalent HPLMN Presentation Indication"});
            put("7fff/5f40/4f49", new String[]{"WHPI", "I-WLAN HPLMN Priority Indication"});
            put("7fff/5f40/4f4a", new String[]{"WLRPLMN", "I-WLAN Last Registered PLMN"});
            put("7fff/5f40/4f4b", new String[]{"HPLMNDAI", "HPLMN Direct Access Indicator"});

            put("7fff/5f50", new String[]{"DFhnb", "Home NodeB Directory"});
            put("7fff/5f50/4f81", new String[]{"ACSGL", "Allowed CSG Lists"});
            put("7fff/5f50/4f82", new String[]{"CSGT", "CSG Type"});
            put("7fff/5f50/4f83", new String[]{"HNBN", "Home NodeB Name"});
            put("7fff/5f50/4f84", new String[]{"OCSGL", "Operator CSG Lists"});
            put("7fff/5f50/4f85", new String[]{"OCSGT", "Operator CSG Type"});
            put("7fff/5f50/4f86", new String[]{"OHNBN", "Operator Home NodeB Name"});

            put("7fff/5f90", new String[]{"DFprose", "ProSe Directory"});
            put("7fff/5f90/4f01", new String[]{"PROSE_MON", "ProSe Monitoring Parameters"});
            put("7fff/5f90/4f02", new String[]{"PROSE_ANN", "ProSe Announcing Parameters"});
            put("7fff/5f90/4f03", new String[]{"PROSEFUNC", "HPLMN ProSe Function"});
            put("7fff/5f90/4f04", new String[]{"PROSE_RADIO_COM", "ProSe Direct Communication Radio Parameters"});
            put("7fff/5f90/4f05", new String[]{"PROSE_RADIO_MON", "ProSe Direct Discovery Monitoring Radio Parameters"});
            put("7fff/5f90/4f06", new String[]{"PROSE_RADIO_ANN", "ProSe Direct Discovery Announcing Radio Parameters"});
            put("7fff/5f90/4f07", new String[]{"PROSE_POLICY", "ProSe Policy Parameters"});
            put("7fff/5f90/4f08", new String[]{"PROSE_PLMN", "ProSe PLMN Parameters"});
            put("7fff/5f90/4f09", new String[]{"PROSE_GC", "ProSe Group Counter"});
            put("7fff/5f90/4f10", new String[]{"PST", "ProSe Service Table"});
            put("7fff/5f90/4f11", new String[]{"PROSE_UIRC", "ProSe UsageInformationReportingConfiguration"});
            put("7fff/5f90/4f12", new String[]{"PROSE_GM_DISCOVERY", "ProSe Group Member Discovery Parameters"});
            put("7fff/5f90/4f13", new String[]{"PROSE_RELAY", "ProSe Relay Parameters"});
            put("7fff/5f90/4f14", new String[]{"PROSE_RELAY_DISCOVERY", "ProSe Relay Discovery Parameters"});

            put("7fff/5fa0", new String[]{"DFacdc", "ACDC Directory"});
            put("7fff/5fa0/4f01", new String[]{"ACDC_LIST", "ACDC List"});
            // FIXME: https://www.etsi.org/deliver/etsi_ts/131100_131199/131102/16.04.00_60/ts_131102v160400p.pdf page 232
            // specifies more files under DF 5f3a, but they have the format of 4FXX. find out what that XX is and implement it...

            put("7fff/5fb0", new String[]{"DFtv", "TV Directory"});
            // FIXME: https://www.etsi.org/deliver/etsi_ts/131100_131199/131102/16.04.00_60/ts_131102v160400p.pdf page 232
            // specifies more files under DF 5f3a, but they have the format of 4FXX. find out what that XX is and implement it...

            put("7fff/5fc0", new String[]{"DF5gs", "5GS Directory"});
            put("7fff/5fc0/4f01", new String[]{"5GS3GPPLOCI", "5GS 3GPP location information"});
            put("7fff/5fc0/4f02", new String[]{"5GSN3GPPLOCI", "5GS non-3GPP location information"});
            put("7fff/5fc0/4f03", new String[]{"5GS3GPPNSC", "5GS 3GPP Access NAS Security Context"});
            put("7fff/5fc0/4f04", new String[]{"5GSN3GPPNSC", "5GS non-3GPP Access NAS Security Context"});
            put("7fff/5fc0/4f05", new String[]{"5GAUTHKEYS", "5G authentication keys"});
            put("7fff/5fc0/4f06", new String[]{"UAC_AIC", "UAC Access Identities Configuration"});
            put("7fff/5fc0/4f07", new String[]{"SUCI_Calc_Info", "Subscription Concealed Identifier Calculation Information EF"});
            put("7fff/5fc0/4f08", new String[]{"OPL5G", "5GS Operator PLMN List"});
            put("7fff/5fc0/4f09", new String[]{"SUPI_NAI", "SUPI as Network Access Identifier"});
            put("7fff/5fc0/4f0a", new String[]{"Routing_Indicator", "Routing Indicator"});
            put("7fff/5fc0/4f0b", new String[]{"URSP", "UE Route Selection Policies"});
            put("7fff/5fc0/4f0c", new String[]{"TN3GPPSNN", "Trusted non-3GPP Serving network names list"});
        }};
    }
}
