package de.srlabs.simlib;

import javax.smartcardio.CardException;

public class EnvelopeSMSPPDownload extends Envelope {

    /* as defined in TS 51.014, Secion 7.1.2 */
    public EnvelopeSMSPPDownload(Address address, SMSTPDU smsTPDU) throws CardException {

        DeviceIdentities device_identities;
        if (SIMLibrary.third_gen_apdu) {
            device_identities = new DeviceIdentities(DeviceIdentities.TYPE_3G, DeviceIdentities.DI_NETWORK, DeviceIdentities.DI_UICC); // Direction Network -> SIM
        } else {
            device_identities = new DeviceIdentities(DeviceIdentities.TYPE_GSM, DeviceIdentities.DI_NETWORK, DeviceIdentities.DI_UICC); // Direction Network -> SIM
        }

        byte[] smspp_data = new byte[device_identities.getBytes().length + address.getBytes().length + smsTPDU.getBytes().length];

        System.arraycopy(device_identities.getBytes(), 0, smspp_data, 0, device_identities.getBytes().length);
        System.arraycopy(address.getBytes(), 0, smspp_data, device_identities.getBytes().length, address.getBytes().length);
        System.arraycopy(smsTPDU.getBytes(), 0, smspp_data, device_identities.getBytes().length + address.getBytes().length, smsTPDU.getBytes().length);

        byte[] smspp = InnerTLV.getInnerTLV((byte) 0xD1, smspp_data);

        super._data = new byte[smspp.length];
        super._data = smspp;
    }
}
