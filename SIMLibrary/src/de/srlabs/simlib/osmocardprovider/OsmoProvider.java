package de.srlabs.simlib.osmocardprovider;

import java.security.Provider;

public class OsmoProvider extends Provider {

    public OsmoProvider() {
        super("OsmoProvider", 1.0d, "Osmocom-BB SIM Provider");
        put("TerminalFactory.OsmoTerminalFactory", "de.srlabs.simlib.osmocardprovider.OsmoSpi");
    }
}
