package de.srlabs.simlib;

import javax.smartcardio.CardException;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

public class OTASMS {

    private final static boolean LOCAL_DEBUG = false;
    private final static boolean DEBUG = Debug.DEBUG || LOCAL_DEBUG;
    private SMSDeliverTPDU _smsdeliver;

    public OTASMS() {
        _smsdeliver = new SMSDeliverTPDU();
        _smsdeliver.setTPUDHI(true);
    }

    public void setTPUD(byte[] bytes) {
        _smsdeliver.setTPUD(bytes);
    }

    public SMSDeliverTPDU getSMSDeliverTPDU() {
        return _smsdeliver;
    }

    public void setSMSDeliverTPDU(SMSDeliverTPDU smsdelivertpdu) {
        _smsdeliver = smsdelivertpdu;
    }

    public void setCommandPacket(CommandPacket cp) {

        if (DEBUG) {
            System.out.println(LoggingUtils.formatDebugMessage("Counter used in this SMS: " + cp.getCounter()));
        }

        _smsdeliver.setTPUD(cp.getBytes());
    }

    public ResponseAPDU send() throws CardException {

        SMSTPDU smstpdu = new SMSTPDU(_smsdeliver.getBytes());

        Address addr;
        if (SIMLibrary.third_gen_apdu) {
            addr = new Address(HexToolkit.fromString("86050021436587"));
        } else {
            addr = new Address(HexToolkit.fromString("06050021436587"));
        }
        
        EnvelopeSMSPPDownload env = new EnvelopeSMSPPDownload(addr, smstpdu);

        CommandAPDU envelope = env.getAPDU();
        if (DEBUG) {
            System.out.println(LoggingUtils.formatDebugMessage("CommandAPDU: " + HexToolkit.toString(envelope.getBytes())));
        }
        ResponseAPDU response = ChannelHandler.transmitOnDefaultChannel(envelope);

        return response;

    }
}
