package de.srlabs.simtester;

public class FuzzerData {
    public String _name;
    public byte _counter;
    public int _kic;
    public int _kid;
    public boolean _requestPoR;
    public boolean _cipherPoR;
    
    public FuzzerData(String name, byte counter, int kic, int kid, boolean requestPoR, boolean cipherPoR) {
        _name = name;
        _counter = counter;
        _kic = kic;
        _kid = kid;
        _requestPoR = requestPoR;
        _cipherPoR = cipherPoR;
    }
}
