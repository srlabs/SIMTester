package de.srlabs.simlib;

import java.util.*;

public class SimCardFileMapping {

    protected Map<String, String[]> getFilesDetails() { return new HashMap<>(); }

    protected String formatPath(String path) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < path.length(); i += 4) {
            result.append(path, i, i + 4);
            if (i != path.length() - 4) {
                result.append("/");
            }
        }
        return result.toString();
    }

    protected String[] getFilenameAndHuman(String formattedPath) {
        /*
        The `formattedPath` represents a path like 3f00/2f00
        The function returns an array with
            [0]: the name of the DF/EF (e.g.: for 3f00/2f00, it returns EFdir)
            [1]: the human readable name of the DF/EF (e.g.: for 3f00/2f00, it returns Application directory)
         */

        String[] fileInfo = new String[]{"N/A", "N/A"};
        if (getFilesDetails().containsKey(formattedPath.toLowerCase())) {
            fileInfo = getFilesDetails().get(formattedPath.toLowerCase());
        }

        return fileInfo;
    }

    public List<SimCardFile> getMappedNameAndDescription(Map<String, SimCardFile> inputFilePaths) {
        List<SimCardFile> results = new ArrayList<>();

        SortedSet<String> sortedKeys = new TreeSet<>(inputFilePaths.keySet());
        for (String path : sortedKeys) {
            String formatted_path = formatPath(path);
            String[] fileInfo = getFilenameAndHuman(formatted_path);

            SimCardFile file = inputFilePaths.get(path);
            file.setFilePath(formatted_path);
            file.setFileName(fileInfo[0]);
            file.setFileDescription(fileInfo[1]);

            results.add(file);
        }

        return results;
    }

    public List<SimCardFile> getMappedNameAndDescription(Map<String, SimCardFile> inputFilePaths, String aid) {
        List<SimCardFile> mappedFiles = getMappedNameAndDescription(inputFilePaths);

        for (SimCardFile file: mappedFiles) {
            // We will return the files as AID/file...
            file.setFilePath(file.getFilePath().replace("7FFF", aid));
        }

        return mappedFiles;
    }
}
