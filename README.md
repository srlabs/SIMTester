# SIMTester

SIMTester assess SIM card security in two dimensions:

- **Cryptanalytic attack surface**: Collect cryptographic signatures and encryptions of known plaintexts
- **Application attack surface**: Generate a list of all application identifiers (TAR) and find "unprotected" (MSL=0) applications

## Running SIMtester

### Prerequisites

- Java 1.7+
- PC/SC reader (via pcsc daemon) **–or–**
- Osmocom phone (via libosmosim)

### Run pre-compile binary

```bash
java -jar SIMTester.jar <arguments>
```

In case Java has problems to find the libpcsclite shared object, just submit the path manually (adjust it to your system):

```bash
java -Dsun.security.smartcardio.library=/lib/x86_64-linux-gnu/libpcsclite.so.1 -jar SIMTester.jar <arguments>
```

## Usage

### General options

| **command line option**                      | description                                                                                |
|----------------------------------------------|--------------------------------------------------------------------------------------------|
| **-vp, --verify-pin <pin>**                  | verifies the PIN, works for CHV1                                                           |
| **-sp, --skip-pin**                          | skips verification of a PIN and ignore permission errors (may produce incomplete results!) |
| **-dp, --disable-pin <pin>**                 | disables PIN, works for CHV1 (ideal for testing cards so no PIN database in needed)        |
| **-tf, --terminal-factory <PCSC/OsmocomBB>** | what are you using as SIM card reader                                                      |
| **-ri, --reader-index <index of a reader>**  | multiple PC/SC readers can be connected, first is 0                                        |
| **-2g, --2g-cmds**                           | Use 2G APDU format only                                                                    |
| **-la, --list-all**                          | Try to connect to all readers and show info about cards in them                            |
| **-nl, --no-logging**                        | Skip the CSV logging                                                                       |
| **-gsmmap, --gsmmap**                        | Automatically contribute data to gsmmap.org                                                |

### Fuzzer

Has its own intelligent logic, fuzzes ~130 chosen TARs, is divided into 3 modes:

- Full fuzzing (default) - all 15 keysets with all 16 fuzzing mechanisms
- Quick fuzzing (**-qf** option) - keysets 1 to 6, only 4 most successful fuzzing mechanisms
- Poke the card (**-poke** option) - same as quick fuzzing but only fuzzes 3 most common TARs (000000, B00001, B00010)

Custom keysets and TARs can be specified via **-t** and **-k** parameters (space being a delimiter between multiple values).

In addition to this the user has the option to keep certain bytes constant during fuzzing:

| command line option |  description                                                  |
|--------------------------|---------------------------------------------------------------|
| **-kic, --kic <arg>**    | Overwrites KIC byte in all fuzzer messages to a custom value  |
| **-kid, --kid <arg>**    | Overwrites KID byte in all fuzzer messages to a custom value  |
| **-spi1, --spi1 <arg>**  | Overwrites SPI1 byte in all fuzzer messages to a custom value |
| **-spi2, --spi2 <arg>**  | Overwrites SPI2 byte in all fuzzer messages to a custom value |

OTA messages are delivered over SMS with standard PID, DCS, UDHI, IEI/CPH parameters. Service providers usually will filter OTA messages by their parameters and will usually comply with the standard to make up their filter rules. Using the OTA-Passthrough fuzzer functionality, the user can check if there are alternative, undocumented parameters which can be used to get the OTA message through. If undocumented parameter constellations are found, its very likely that they are not filtered by the service provider, which would be a weakness.

| command line option              |  description                                                                                  |
|----------------------------------|-----------------------------------------------------------------------------------------------|
| **-of, --ota-fuzz**              | Fuzz OTA passthrough (PID, DCS, UDHI, IEI/CPH)                                                |
| **-ofbf, --ota-fuzz-bruteforce** | Use 0-255 values for both PID and DCS, without this options only most common values are used. |

The response handling of OTA-Messages can be implemented in two different ways. The backend either expects an additional user data field in the SMS-DELIVER-REPORT or waits for an incoming SMS that the card issues (SMS-SUBMIT) after completing the requested operation. The user can use the following option to choose between these to methods:

| command line option            |  description                                         |
|--------------------------------|------------------------------------------------------|
| **-sdr, --sms-deliver-report** | Use SMS-DELIVER-REPORT instead of SMS-SUBMIT for PoR |

### TAR Scanner

Scans for valid TAR values by sending messages to them, has 3 modes:

- Full scan (**-st** option) - scans for all possible TAR values (0x000000 - 0xFFFFFF) - may take a few hours or several days depending on your SIM card speed
- Ranged scan (**-str** option) - scans for valid TAR values in pre-specified ranges to optimise the scanning duration

In addition to the scan type, some options can be specified:

- Smart TAR scan (scan a few random TARs to determine false response, option **-stbs**)

Use a specified REGEXP to match response for false positives (option **-stre**).  
Normally false responses should be constant (PoR status or status word), but there are corner cases in which also false responses might contain variable data.

A starting value for Full scan can be specified using **-t** option.  
A keyset used for sending messages can be specified using **-k** option (for both Full and Ranged scans).

*Tip*: run fuzzer first, see what keysets seem responsive (give answers other than none) and use one of those for TAR scanning, because if you use an inactive keyset it's very probable the card will NOT answer even on a valid TAR which makes TAR scanning non-functional.

### APDU Scanner

Scans for valid APDU values (think of APDUs as of commands to the card) on TARs without any public APDU reference, it has 2 modes:

- LEVEL 1 scan (performed automatically after **Fuzzer** finishes and has found unprotected TARs with responses) - only scans for valid CLA 0x00 - 0xFF - it is performed via OTA messages. Can also be invoked manually (option **-sa**)
- LEVEL 2 scan (option **-sa -sal2**) - scans for both CLA 0x00 - 0xFF and INS 0x00 - 0xFF - it is performed locally on card I/O on initially selected application

### File Scanner

Automatically scans the files present on the SIM. This option can be used to detect non standard, proprietary files on the SIM. The scanner will start at 3F00 and automatically skips reserved values. The user has the option to add additional reserved values in order to skip files in case of problems. It is also possible to run the scanner in standard-scan-mode where only standard files are checked for their presence.

| command line option                     |  description                                                                                                 |
|-----------------------------------------|--------------------------------------------------------------------------------------------------------------|
| **-sf, --scan-files**                   | Scans files on the SIM, starts at MF (0x3F00)                                                                |
| **-sfb, --scan-files-break**            | Use with -sf, stop scanning directory when the count returned by Select APDU matched count of found files    |
| **-sffs, --scan-files-follow-standard** | Use with -sf, only search for IDs that are standardized, eg. 3rd level files only between 4F00 and 4FFF etc. |
| **-sfrv, --sfrv <sfrv>**                | File scanning: Add a file ID to reserved values for file scanning (will be skipped).                         |

## Contribute

### Upload to gsmmap.org

Upload the results to [gsmmap](https://gsmmap.org/) by using the **-gsmmap** option.

Tor can also be specified:

```bash
java -jar SIMTester.jar -gsmmap -socksProxyHost=127.0.0.1 -socksProxyPort=<tor_port> ... other options ...
```

If you already have scanned your cards without **-gsmmap** option, you can use the [web form](http://gsmmap.org/upload.html) to upload your CSV results provided by SIMTester.