package de.srlabs.simtester;

import de.srlabs.simlib.*;

import javax.smartcardio.CardException;
import java.io.FileNotFoundException;
import java.util.*;

public class FileScanner {

    private final static boolean LOCAL_DEBUG = false;
    private final static boolean DEBUG = Debug.DEBUG || LOCAL_DEBUG;

    public static List<String> userDefinedReservedIDs = new ArrayList<>();

    private static Map<String, SimCardFile> scanSimFromPath(String startingDF,
                                                            boolean breakAfterCountsMatch,
                                                            boolean lazyScan,
                                                            boolean scanAID) throws CardException, FileNotFoundException {
        return scanSimFromPath(startingDF, breakAfterCountsMatch, lazyScan, new ArrayList<>(), new ArrayList<>(), scanAID);
    }

    /**
     * @param startingDF:            the DF where the scan starts; e.g.: 3F00 or 3F007F10 or 7FFF...
     * @param breakAfterCountsMatch: exit the function if the count DF and EF matches (FIXME: 3G APDU do not return the DF/EF count, so this is useless)
     * @param lazyScan:              scan just by the possible values range defined in the standard
     * @param aboveLevelFiles:       a list with FID of the files that are on the above level as startingDF
     * @param sameLevelFiles:        a list with FID of the files that are on the same level as startingDF
     * @param scanAID:               this is `true` if we scan the files of an AID
     * @return Map<String, SimCardFile>: the String is the path of the file and SimCardFile is the file object
     * @throws CardException
     * @throws FileNotFoundException
     */
    private static Map<String, SimCardFile> scanSimFromPath(String startingDF,
                                                            boolean breakAfterCountsMatch,
                                                            boolean lazyScan,
                                                            List<String> aboveLevelFiles,
                                                            List<String> sameLevelFiles,
                                                            boolean scanAID) throws CardException, FileNotFoundException {
        String currentDir;

        List<String> dirScan = new ArrayList<>();
        dirScan.add(startingDF);

        Map<String, SimCardFile> results = new HashMap<>();

        // These are standard values that should not be scanned
        // They also contain of FIDs of AIDs if there are any
        List<String> reservedValues = new ArrayList<>(Arrays.asList(
                "3F00",
                "3FFF",     // seems to be an alias for 3F00
                "7FFF",     // root directory to refer to an AID
                "FFFF"      // seems to point to 3F00
        ));
        reservedValues.addAll(userDefinedReservedIDs);

        // This list keeps the FIDs of the discovered EF and DF
        // It is used as `sameLevelFiles` param for the next iteration of the function (recursive function)
        List<String> resultsFIDs = new ArrayList<>();
        // Add the FID of the current DF
        resultsFIDs.add(startingDF.substring(startingDF.length() - 4));

        if (DEBUG && !userDefinedReservedIDs.isEmpty()) {
            System.out.println(LoggingUtils.formatDebugMessage("File scanning added the following user-defined reserved file IDs: " + userDefinedReservedIDs));
        }

        System.out.println("Reserved values are: " + reservedValues);
        System.out.println("FIDs from above level: " + aboveLevelFiles);
        System.out.println("FIDs from same level: " + sameLevelFiles);

        currentDir = dirScan.get(0);
        SimCardFile current;

        if (scanAID) {
            current = FileManagement.selectFileByPath(currentDir);
        } else {
            current = FileManagement.selectPath(currentDir);
        }

        int hasDFs = current.getNumberOfChildDFs();
        int hasEFs = current.getNumberOfChildEFs();

        System.out.println("[" + currentDir + "] Should have " + hasDFs + " directories and " + hasEFs + " files.");

        results.put(currentDir, current);

        int foundDFs = 0;
        int foundEFs = 0;

        String filePath;
        boolean lastSelectSuccessful = false;

        for (int i = 0; i <= 65535; i++) { // 65535 as fileId = FFFF

            if (lazyScan) {
                int level = currentDir.length() / 4;
                if (level == 1) {
                    if ((i < Integer.parseInt("2F00", 16) || i > Integer.parseInt("2FFF", 16)) && (i < Integer.parseInt("7F00", 16) || i > Integer.parseInt("7FFF", 16))) {
                        continue;
                    }
                } else if (level == 2) {
                    if ((i < Integer.parseInt("5F00", 16) || i > Integer.parseInt("5FFF", 16)) && (i < Integer.parseInt("6F00", 16) || i > Integer.parseInt("6FFF", 16))) {
                        continue;
                    }
                } else if (level == 3) {
                    if (i < Integer.parseInt("4F00", 16) || i > Integer.parseInt("4FFF", 16)) {
                        continue;
                    }
                }
            }

            String fileId = String.format("%04X", i);

            if (reservedValues.contains(fileId) || aboveLevelFiles.contains(fileId) || sameLevelFiles.contains(fileId)) {
                if (DEBUG) {
                    System.out.println(LoggingUtils.formatDebugMessage("======= RESERVED VALUE " + fileId + " FOUND, SKIPPING!! ======="));
                }
                continue;
            }

            if (lastSelectSuccessful || scanAID) {
                filePath = currentDir + fileId;
            } else {
                filePath = fileId;
            }

            if ((i % 100) == 0) {
                System.out.println("[" + currentDir + "] currently checking: " + fileId);
            }

            try {
                SimCardFile file;
                if (scanAID) {
                    file = FileManagement.selectFileByPath(filePath);
                } else {
                    file = FileManagement.selectPath(filePath);
                }

                // We found a case where we selected 7F21 and the FID in the response was 7F20
                // If we already have the FID in our results, we will ignore it
                if (resultsFIDs.contains(file.getFileId())) {
                    resultsFIDs.add(fileId);
                    continue;
                } else if (!file.getFileId().equals(fileId)) {
                    // Return an exception if we do not have the FID in our results and the requested FID and the returned one are different
                    throw new CardException(String.format("The selected FID (%s) does not match with the file FID (%s)", fileId, file.getFileId()));
                }

                System.out.println("[" + currentDir + "] File FOUND, id: " + file.getFileId() + ", type: " + file.getFileTypeName());

                if (file.getFileType() == SimCardFile.EF || file.getFileType() == SimCardFile.INTERNAL_EF) {
                    foundEFs++;
                    results.put(currentDir + fileId, file);
                    resultsFIDs.add(fileId);
                    if (DEBUG) {
                        System.out.println("[" + currentDir + "] Found EFs: " + foundEFs + "/" + hasEFs);
                    }
                } else if (file.getFileType() == SimCardFile.DF) {
                    foundDFs++;
                    dirScan.add(currentDir + fileId); // add newly found dir to map, so it will be scanned.
                    results.put(currentDir + fileId, file);
                    resultsFIDs.add(fileId); //this is needed, because if we're in 3F00/7F20 and we're trying to select eg. 7F10 it will jump to 3F00/7F10, meaning such combination is impossible to have on the card anyway
                    System.out.println("Found FIDs are: " + resultsFIDs);
                    if (DEBUG) {
                        System.out.println(LoggingUtils.formatDebugMessage("[" + currentDir + "] Found DFs: " + foundDFs + "/" + hasDFs));
                    }
                } else if (file.getFileType() == SimCardFile.ADF) {
                    // We found an ADF, so we will just add it to userDefinedReservedIDs
                    System.out.println("ADF found: " + fileId);
                    userDefinedReservedIDs.add(fileId);
                } else {
                    throw new CardException("File is not EF or DF. It is: " + file.getFileTypeName() + ". File path: " + filePath);
                }

                if (breakAfterCountsMatch && foundEFs == hasEFs && foundDFs == hasDFs) {
                    System.out.println("[" + currentDir + "] Fuck this shit, already got all DFs and EFs, no point in scanning any further");
                    break;
                }

                lastSelectSuccessful = true; // we have to select parent dir again for next iteration, if this was a directory not select parent directory again could create a mess

            } catch (FileNotFoundException e) {
                lastSelectSuccessful = false; // such file does NOT exist, jump to next one and set this to false so we don't have to select parent dir again (MUCH faster)
            }
        } // end for
        System.out.println("[" + currentDir + "] STATUS: found DFs: " + foundDFs + "/" + hasDFs + ", found EFs: " + foundEFs + "/" + hasEFs);
        System.out.println("Files found: " + resultsFIDs);
        System.out.println();
        dirScan.remove(currentDir);

        // Call the recurse function for all the DFs found in the startinfDF
        while (!dirScan.isEmpty()) {
            results.putAll(scanSimFromPath(dirScan.remove(0), breakAfterCountsMatch, lazyScan, sameLevelFiles, resultsFIDs, scanAID));
        } // end while

        return results;
    }

    public static String getFIDforAID(String aid) throws CardException {
        System.out.println();
        System.out.println("\033[96mSearch for FID of AID " + aid + "\033[0m");

        List<String> reservedValues = new ArrayList<>(Arrays.asList(
                "3F00",
                "3FFF",     // seems to be an alias for 3F00
                "7FFF",     // root directory to refer to an AID
                "FFFF"      // seems to point to 3F00
        ));

        // Select AID
        byte[] fid;
        try {
            fid = FileManagement.selectAID(HexToolkit.fromString(aid));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }

        if (fid != null) {
            return HexToolkit.toString(fid);
        }

        // For all options
        for (int i = 0; i < 65535; i++) {
            // Select each file
            String fileId = String.format("%04X", i);

            if (reservedValues.contains(fileId)) {
                continue;
            }

            if (i % 500 == 0) {
                System.out.println("Currently checking: " + fileId);
            }

            try {
                SimCardFile file = FileManagement.selectPath(fileId);

                if (file.getFileType() == SimCardFile.ADF) {
                    return fileId;
                }
            } catch (FileNotFoundException ignored) {
            }
        }

//        throw new CardException("The FID for AID " + aid + " was not found");
        return null;
    }

    public static void scanSim(boolean breakAfterCountsMatch, boolean lazyScan, CSVWriter writer) throws CardException, FileNotFoundException {
        Map<String, SimCardFile> scanFileResults;   // store the data returned by scanSimFromPath
        List<SimCardFile> finalResults = new ArrayList<>();    // store the data processed by SimCardFileMapping
        ArrayList<String> aids = new ArrayList<>();

        // Write CSV header
        writer.writeRawLine("# path,type,size,name,humanly_readable");

        if (SIMLibrary.third_gen_apdu) {
            // TODO: check if you really need to do a reset here or not
            ChannelHandler.getInstance().reset();
            // Get all the AIDs from the card
            aids = CommonFileReader.getAIDs();
            // Iterate AIDs and get FIDs
            for (String aid : aids) {
                String fid = getFIDforAID(aid);
                if (fid == null) {
                    System.out.println("\033[96mNo FID found. Maybe you want to manually check it\033[0m");
                } else {
                    System.out.println("FID found: " + fid);
                }
                // Add the FID of AID such us it will not be selected while brute-forcing
                userDefinedReservedIDs.add(fid);
            }
            ChannelHandler.getInstance().reset();
        }

        // Search for 2G files
        System.out.println("");
        System.out.println("\033[96mReading files from MF\033[0m");
        scanFileResults = scanSimFromPath("3F00", breakAfterCountsMatch, lazyScan, false);
        // Process the data (add file name and description, order by path
        finalResults.addAll(new MFSimCardFileMapping().getMappedNameAndDescription(scanFileResults));

        if (SIMLibrary.third_gen_apdu) {
            // Search for 3G files
            for (String aid : aids) {
                System.out.println("\033[96mSelecting the AID " + aid + "\033[0m");
                try {
                    HexToolkit.toString(FileManagement.selectAID(HexToolkit.fromString(aid)));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    continue;
                }
                scanFileResults = scanSimFromPath("7FFF", breakAfterCountsMatch, lazyScan, true);

                // Check the type of the AID (USIM, ISIM, etc)
                // Based on the type, you should create the SimCardFileMapping class
                // Defined in https://www.etsi.org/deliver/etsi_ts/101200_101299/101220/12.00.00_60/ts_101220v120000p.pdf (Table E.1)
                if (aid.startsWith("A0000000871002")) {
                    // USIM app
                    finalResults.addAll(new USIMCardFileMapping().getMappedNameAndDescription(scanFileResults, aid));
                } else if (aid.startsWith("A0000000871004")) {
                    // ISIM app
                    finalResults.addAll(new ISIMCardFileMapping().getMappedNameAndDescription(scanFileResults, aid));
                } else {
                    finalResults.addAll(new SimCardFileMapping().getMappedNameAndDescription(scanFileResults, aid));
                }
            }
        }

        finalResults.forEach((file) -> {
            System.out.printf("%-20s %4d %-15s %s\n", file.getFilePath(), file.getFileSize(), file.getFileName(), file.getFileDescription());
            writer.writeRawLine(String.format(
                    "%s,%s,%s,%s,%s",
                    file.getFilePath(),
                    file.getFileTypeName(),
                    file.getFileSize(),
                    file.getFileName(),
                    file.getFileDescription()
                    )
            );
        });
    }
}
