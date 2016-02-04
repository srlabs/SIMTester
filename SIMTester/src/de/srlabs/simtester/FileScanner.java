package de.srlabs.simtester;

import de.srlabs.simlib.Debug;
import de.srlabs.simlib.FileManagement;
import de.srlabs.simlib.LoggingUtils;
import de.srlabs.simlib.SimCardFile;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.smartcardio.CardException;

public class FileScanner {

    private final static boolean LOCAL_DEBUG = false;
    private final static boolean DEBUG = Debug.DEBUG || LOCAL_DEBUG;

    public static List<String> userDefinedReservedIDs = new ArrayList<>();

    public static void scanSim(boolean breakAfterCountsMatch, boolean lazyScan) throws CardException, FileNotFoundException {

        String currentDir;

        List<String> dirScan = new ArrayList<>();
        dirScan.add("3F00");

        Map<String, String> results = new HashMap<>();

        List<String> reservedValues = new ArrayList<>(Arrays.asList(new String[]{"3F00", "3FFF"/* seems to be an alias for 3F00 */, "7FFF"}));
        reservedValues.addAll(userDefinedReservedIDs);

        if (DEBUG && !userDefinedReservedIDs.isEmpty()) {
            System.out.println(LoggingUtils.formatDebugMessage("File scanning added the following user-defined reserved file IDs: " + userDefinedReservedIDs));
        }

        while (!dirScan.isEmpty()) { // while we have unscanned directories

            currentDir = (String) dirScan.get(0);

            SimCardFile current = FileManagement.selectPath(currentDir);

            int hasDFs = current.getNumberOfChildDFs();
            int hasEFs = current.getNumberOfChildEFs();

            System.out.println("[" + currentDir + "] Should have " + hasDFs + " directories and " + hasEFs + " files.");

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

                if (reservedValues.contains(fileId)) {
                    if (DEBUG) {
                        System.out.println(LoggingUtils.formatDebugMessage("======= RESERVED VALUE " + fileId + " FOUND, SKIPPING!! ======="));
                    }
                    continue;
                }

                if (lastSelectSuccessful) {
                    filePath = currentDir + fileId;
                } else {
                    filePath = fileId;
                }

                if ((i % 100) == 0) {
                    System.out.println("[" + currentDir + "] currently checking: " + filePath);
                }

                try {
                    SimCardFile file = FileManagement.selectPath(filePath);
                    System.out.println("[" + currentDir + "] File FOUND, id: " + file.getFileId() + ", type: " + file.getFileTypeName());
                    if (file.getFileType() == SimCardFile.EF) {
                        foundEFs++;
                        results.put(currentDir + fileId, null);
                        if (DEBUG) {
                            System.out.println("[" + currentDir + "] Found EFs: " + foundEFs + "/" + hasEFs);
                        }
                    } else {
                        foundDFs++;
                        dirScan.add(currentDir + fileId); // add newly found dir to map, so it will be scanned.
                        results.put(currentDir + fileId, null);
                        reservedValues.add(fileId); //this is needed, because if we're in 3F00/7F20 and we're trying to select eg. 7F10 it will jump to 3F00/7F10, meaning such combination is impossible to have on the card anyway
                        System.out.println("Reserved values are now: " + reservedValues);
                        if (DEBUG) {
                            System.out.println(LoggingUtils.formatDebugMessage("[" + currentDir + "] Found DFs: " + foundDFs + "/" + hasDFs));
                        }
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
            System.out.println();
            dirScan.remove(currentDir);
        } // end while

        System.out.println();
        System.out.println("============ RESULTS ============");
        System.out.println();

        SortedSet<String> sortedKeys = new TreeSet<>(results.keySet());

        System.out.printf("%-20s %4s %-15s %s\n", "file path", "size", "file name", "file description");

        Iterator iterator = sortedKeys.iterator();
        while (iterator.hasNext()) {
            String path = iterator.next().toString();
            String formatted_path = formatPath(path);
            SimCardFile current = FileManagement.selectPath(path);
            /*if (current.getFileType() == SimCardFile.EF) {
             System.out.print(" - " + current.getFileTypeName() + ", size: " + current.getFileSize());
             } else {
             System.out.print(" - " + current.getFileTypeName() + ", DFs: " + current.getNumberOfChildDFs() + ", EFs: " + current.getNumberOfChildEFs());
             }*/
            String[] fileInfo = new String[]{"N/A", "N/A"};
            if (SimCardFile._fileMap.containsKey(formatted_path.toLowerCase())) {
                fileInfo = SimCardFile._fileMap.get(formatted_path.toLowerCase());
            }

            System.out.printf("%-20s %4d %-15s %s\n", formatted_path, current.getFileSize(), fileInfo[0], fileInfo[1]);
        }
    }

    public static String formatPath(String path) {
        String result = new String();
        for (int i = 0; i < path.length(); i += 4) {
            result += path.substring(i, i + 4);
            if (i != path.length() - 4) {
                result += "/";
            }
        }
        return result;
    }
}
