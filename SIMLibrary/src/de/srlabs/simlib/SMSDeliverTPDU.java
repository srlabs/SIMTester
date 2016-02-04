package de.srlabs.simlib;

/* TP 23.040
 * 9.2.2.1	SMS DELIVER type
 * Basic elements of the SMS DELIVER type:
 * Abbr.	Reference                       P1)	R2)	Description
 * TP-MTI	TP Message Type Indicator	M	2b	Parameter describing the message type.
 * TP-MMS	TP More Messages to Send	M	b	Parameter indicating whether or not there are more messages to send
 * TP-LP	TP-Loop-Prevention              O	b	Parameter indicating that SMS applications should inhibit forwarding or automatic message generation that could cause infinite looping.
 * TP-RP	TP Reply Path                   M	b	Parameter indicating that Reply Path exists.
 * TP-UDHI	TP User Data Header Indicator	O	b	Parameter indicating that the TP UD field contains a Header
 * TP-SRI	TP Status Report Indication	O	b	Parameter indicating if the SME has requested a status report.
 *
 * TP-OA	TP Originating Address          M	2 12o	Address of the originating SME.
 * TP-PID	TP Protocol Identifier          M	o	Parameter identifying the above layer protocol, if any.
 * TP-DCS	TP Data Coding Scheme           M	o	Parameter identifying the coding scheme within the TP User Data.
 * TP-SCTS	TP Service Centre Time Stamp	M	7o	Parameter identifying time when the SC received the message.
 * TP-UDL	TP User Data Length             M	I	Parameter indicating the length of the TP User Data field to follow.
 *
 * TP-UD	TP User Data                    O	3)	
 * 
 * 1)	Provision;		Mandatory (M) or Optional (O).
 * 2)	Representation;	Integer (I), bit (b), 2 bits (2b), Octet (o), 7 octets (7o), 2 12 octets (2 12o).
 * 3)	Dependent on the TP DCS.
 */
public class SMSDeliverTPDU {

    private final static boolean LOCAL_DEBUG = false;
    private final static boolean DEBUG = Debug.DEBUG || LOCAL_DEBUG;

    /* first Octet description 
     * bit 0 - TP-MTI
     * bit 1 - TP-MTI
     * bit 2 - TP-MMS
     * bit 3 - TP-LP
     * bit 4 - not used
     * bit 5 - TP-SRI
     * bit 6 - TP-UDHI
     * bit 7 - TP-RP
     */

    /* 9.2.3.1	TP Message Type Indicator (TP MTI)
     * The TP-Message-Type-Indicator is a 2-bit field, located within bits no 0 and 1 of the first octet of all PDUs which can be given the following values:
     * bit1	bit0	Message type
     * 0	0	SMS DELIVER (in the direction SC to MS)
     * 
     */
    private final static byte TPMTI = (byte) 0x0;
    private final static byte TPMTI_MASK = (byte) 0x3; // bit no 0 and 1
    /*
     * 9.2.3.2	TP More Messages to Send (TP MMS)
     * The TP More Messages to Send is a 1 bit field, located within bit no 2 of the first octet of SMS DELIVER and SMS STATUS REPORT, and to be given the following values:
     * Bit no 2:	0		More messages are waiting for the MS in this SC
     *                  1		No more messages are waiting for the MS in this SC
     * NOTE:	In the case of SMS STATUS REPORT this parameter refers to messages waiting for the mobile to which the status report is sent. The term message in this context refers to SMS messages or status reports.
     */
    private byte TPMMS = (byte) 0x4; // 0x0 - true (2nd bit = 0), 0x2 - false (2nd bit = 1)
    private final static byte TPMMS_MASK = (byte) 0x4; // bit no 2

    public boolean getTPMMS() {
        return !HexToolkit.isBitSet(TPMMS, 2);
    }

    public void setTPMMS(boolean more_messages_waiting) {
        if (more_messages_waiting) {
            TPMMS = (byte) 0x0;
        } else {
            TPMMS = (byte) 0x4;
        }

    }
    /* 9.2.3.28	TP Loop-Prevention (TP LP)
     * The TP Loop-Prevention is a 1 bit field, located within bit no 3 of the first octet of the SMS Deliver and SMS-Status-Report, and to be given the values in the table below. 
     * TP-LP Value	Description
     *          0       The message has not been forwarded and is not a spawned message (or the sending network entity (e.g. an SC) does not support the setting of this bit.)
     *          1       The message has either been forwarded or is a spawned message.
     */
    private byte TPLP = (byte) 0x0;
    private final static byte TPLP_MASK = (byte) 0x8; // bit no. 3

    public boolean getTPLP() {
        return !HexToolkit.isBitSet(TPLP, 3);
    }

    public void setTPLP(boolean has_been_forwarded) {
        if (has_been_forwarded) {
            TPLP = (byte) 0x8;
        } else {
            TPLP = (byte) 0x0;
        }

    }
    /* 9.2.3.4	TP Status Report Indication (TP SRI)
     * The TP Status Report Indication is a 1 bit field, located within bit no. 5 of the first octet of SMS DELIVER, and to be given the following values:
     * Bit no. 5:	0		A status report shall not be returned to the SME	
     *                  1		A status report shall be returned to the SME
     */
    private byte TPSRI = (byte) 0x0;
    private final static byte TPSRI_MASK = (byte) 0x20; // bit no. 5

    public boolean getTPSRI() {
        return !HexToolkit.isBitSet(TPSRI, 5);
    }

    public void setTPSRI(boolean status_report_shall_be_returned) {
        if (status_report_shall_be_returned) {
            TPSRI = (byte) 0x20;
        } else {
            TPSRI = (byte) 0x0;
        }

    }
    /* 9.2.3.23	TP User Data Header Indicator (TP UDHI)
     * The TP User Data Header Indicator is a 1 bit field within bit 6 of the first octet 
     * TP-UDHI has the following values.
     * Bit no. 6	0	The TP UD field contains only the short message
     *                  1	The beginning of the TP UD field contains a Header in addition to the short message.
     */
    private byte TPUDHI = (byte) 0x0;
    private final static byte TPUDHI_MASK = (byte) 0x40; // bit no. 6

    public boolean getTPUDHI() {
        return HexToolkit.isBitSet(TPUDHI, 6);
    }

    public void setTPUDHI(boolean contains_user_data_header) {
        if (contains_user_data_header) {
            TPUDHI = (byte) 0x40;
        } else {
            TPUDHI = (byte) 0x0;
        }

    }
    /* 9.2.3.17	TP Reply Path (TP RP)
     * The TP Reply Path is a 1 bit field, located within bit no 7 of the first octet of both SMS DELIVER and SMS SUBMIT, and to be given the following values:
     * Bit no 7:		0		TP Reply Path parameter is not set in this SMS SUBMIT/DELIVER
     *                          1		TP Reply Path parameter is set in this SMS SUBMIT/DELIVER
     */
    private byte TPRP = (byte) 0x0;
    private final static byte TPRP_MASK = (byte) 0x80; // bit no. 7

    public boolean getTPRP() {
        return HexToolkit.isBitSet(TPLP, 7);
    }

    public void setTPRP(boolean reply_path_is_set) {
        if (reply_path_is_set) {
            TPRP = (byte) 0x80;
        } else {
            TPRP = (byte) 0x0;
        }

    }

    public byte getFirstOctet() {
        byte output;

        output = (byte) (((byte) TPMTI & (byte) TPMTI_MASK)
                | ((byte) TPMMS & (byte) TPMMS_MASK)
                | ((byte) TPLP & (byte) TPLP_MASK)
                | ((byte) TPSRI & (byte) TPSRI_MASK)
                | ((byte) TPUDHI & (byte) TPUDHI_MASK)
                | ((byte) TPRP & (byte) TPRP_MASK));

        return output;
    }

    public void setFirstOctet(byte firstOctet) {
        // TPMTI - cannot be set, bit 0 and 1 are hardcoded as they define the SMS-DELIVER type
        TPMMS = (byte) ((byte) firstOctet & (byte) TPMMS_MASK);
        TPLP = (byte) ((byte) firstOctet & (byte) TPLP_MASK);
        TPSRI = (byte) ((byte) firstOctet & (byte) TPSRI_MASK);
        TPUDHI = (byte) ((byte) firstOctet & (byte) TPUDHI_MASK);
        TPRP = (byte) ((byte) firstOctet & (byte) TPRP_MASK);
    }
    /* 9.2.3.7	TP Originating Address (TP OA)
     * The TP Originating Address field is formatted according to the formatting rules of address fields.
     * The first ‘#’ encountered in TP-OA indicates where the address for SMSC routing purposes is terminated. Additional ‘*’s or ‘#’s can be present in the following digits, and all these digits including the first ‘#’ are subaddress digits.
     */
    private byte[] TPOA = new byte[]{(byte) 0x05, (byte) 0x00, (byte) 0x21, (byte) 0x43, (byte) 0xF5}; // number of nibbles, TON_NPI byte, nibbles data

    public byte[] getTPOA() {
        return TPOA;
    }

    public void setTPOA(byte[] tpoa_bytes) throws Exception {
        throw new Exception("not yet implemented!");
        /*
         * TPOA = new byte[2 + tpoa_bytes.length];
         * TPOA[0] = number_of_nibbles(tpoa_bytes); // we need to strip 0xF and count valid nibbles only!
         * TPOA[1] = (byte) 0x00; // TON_NPI byte, 0x00 - TON unknown, NPI unknown (should be sufficient for our use)
         * System.arraycopy(tpoa_bytes, 0, TPOA, 2, tpoa_bytes.length);
         */
    }
    /* 9.2.3.9	TP Protocol Identifier (TP PID)
     * as we currently only need implementation of (U)SIM Data Download for Envelope purposes, this does NOT contain the full implementation of TP-PID
     * In the case where bit 7 = 0, bit 6 = 1, bits 5..0 are used as defined below
     * 111111		(U)SIM Data download
     * Therefore TP-PID for (U)SIM Data Download is defined as (b7-b0) 0111 1111 = 7F (hex)
     */
    private byte TPPID = (byte) 0x7F;
    /* 9.2.3.10	TP Data Coding Scheme (TP DCS)
     * The TP Data Coding Scheme is defined in 3GPP TS 23.038 [9].
     * as we want 8-bit data encoding and our envelope is going to be (U)SIM specific message bits are defined as:
     * 1111 0110 as stated in Section 4 in TS 23.038 (SMS Data Coding Scheme)
     */

    public byte getPID() {
        return TPPID;
    }

    public void setPID(byte pid) {
        TPPID = pid;
    }

    private byte TPDCS = (byte) 0xF6;
    /* 9.2.3.11	TP Service Centre Time Stamp (TP SCTS)
     * The TP Service Centre Time Stamp field is given in semi octet representation, and represents the local time in the following way:
     *                 Year:	Month:	Day:	Hour:	Minute:	Second:	Time Zone:
     * Digits:
     * (Semi octets)	2	2	2	2	2	2	2
     * 
     * as we don't really care about this parameter we'll fill it with 0x00, it's not important for our data messages (at least not for testing)
     */

    public byte getDCS() {
        return TPDCS;
    }

    public void setDCS(byte dcs) {
        TPDCS = dcs;
    }

    private byte[] TPSCTS = new byte[7];
    /* 9.2.3.16	TP User Data Length (TP UDL)
     * If the TP User Data is coded using 8 bit data, the TP User Data Length field gives an integer representation of the number of octets within the TP User Data field to follow.
     */
    private byte TPUDL = (byte) 0x00;

    public int getTPUDL() {
        return TPUDL;
    }
    
    public void setFakeTPUDL(byte length) {
        TPUDL = (byte) length;
    }
    
    private byte[] TPUD = new byte[0];

    public void setTPUD(byte[] userData) {
        if (DEBUG) {
            System.out.println(LoggingUtils.formatDebugMessage("raw data: " + HexToolkit.toString(userData)));
        }
        TPUD = new byte[userData.length];
        System.arraycopy(userData, 0, TPUD, 0, userData.length);
        TPUDL = (byte) userData.length;
    }

    public byte[] getBytes() {
        byte[] output = new byte[1 + TPOA.length + 1 + 1 + TPSCTS.length + 1 + TPUD.length];
        output[0] = getFirstOctet();
        System.arraycopy(TPOA, 0, output, 1, TPOA.length);
        output[TPOA.length + 1] = TPPID;
        output[TPOA.length + 2] = TPDCS;
        System.arraycopy(TPSCTS, 0, output, TPOA.length + 3, TPSCTS.length);
        output[TPOA.length + 3 + TPSCTS.length] = TPUDL;
        System.arraycopy(TPUD, 0, output, TPOA.length + 3 + TPSCTS.length + 1, TPUD.length);

        return output;
    }
}
