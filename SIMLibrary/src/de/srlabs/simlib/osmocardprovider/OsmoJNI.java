package de.srlabs.simlib.osmocardprovider;

public class OsmoJNI {

    static public void loadLib() {
        try {
            System.loadLibrary("osmosim");
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    // #define LOGL_DEBUG      1 
    // #define LOGL_INFO       3
    // #define LOGL_NOTICE     5 
    // #define LOGL_ERROR      7 
    // #define LOGL_FATAL      8 
    public native int loglevel(int loglevel);
    public native boolean init(); // connect to layer2, set up logging and prepare everything.. opposite to exit
    public native byte[] simPowerup(); // power up the sim card (actual voltage)
    public native boolean simReset(); // returns true/false -> is sim present in the phone?
    public native void simPowerdown(); // power down the sim card (actual voltage)
    public native byte[] transmit(byte[] apdu); // transmit APDU buffer and get response buffer back
    public native void exit(); // close layer2 and destroy everything that was initialized in init()
}
