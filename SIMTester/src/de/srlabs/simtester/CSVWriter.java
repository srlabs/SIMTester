package de.srlabs.simtester;

import de.srlabs.simlib.CommandPacket;
import de.srlabs.simlib.HexToolkit;
import de.srlabs.simlib.LoggingUtils;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class CSVWriter {

    private FileWriter _writer;
    private boolean _headerWritten = false;
    private boolean _logging = true;
    private File _csvfile;

    public CSVWriter(String ICCID, String type, boolean logging) {
        _logging = logging;
        if (_logging) {
            _csvfile = new File("." + type + "_" + ICCID + "_" + System.currentTimeMillis() + ".csv");
            try {
                _writer = new FileWriter(_csvfile);
            } catch (IOException e) {
                System.err.println(LoggingUtils.formatDebugMessage("Unable to create file " + _csvfile.getName() + ", exiting.."));
                e.printStackTrace(System.err);
                System.exit(1);
            }
        }
    }
    
    public boolean unhideFile() {
        if (_csvfile.getName().startsWith(".")) {
            File newFile = new File(_csvfile.getName().substring(1));
            return _csvfile.renameTo(newFile);
        } else {
            return false;
        }
    }
    
    public String getFileName() {
        return _csvfile.getName();
    }

    private void writeHeader() throws IOException {
        if (_logging) {
            _writer.append("# ");
            _writer.append("id");
            _writer.append(',');
            _writer.append("Command data");
            _writer.append(',');
            _writer.append("Response data");
            _writer.append('\n');

            _writer.flush();
        }
    }

    public void writeBasicInfo(String ATR, String ICCID, String IMSI, String EF_MANUAREA, String EF_DIR, String AppDeSelect) {
        if (_logging) {
            try {
                _writer.append("ATR:" + ATR + '\n');
                _writer.append("ICCID:" + ICCID + '\n');
                _writer.append("IMSI:" + IMSI + '\n');
                _writer.append("EF_MANUAREA:" + EF_MANUAREA + '\n');
                _writer.append("EF_DIR:" + EF_DIR + '\n');
                _writer.append("AppDeSelect:" + AppDeSelect + '\n');

                _writer.flush();
            } catch (IOException e) {
                System.err.println(LoggingUtils.formatDebugMessage("Unable to write basic info into the CSV file, something's wrooooong, panic, panic, exit."));
                e.printStackTrace(System.err);
                System.exit(1);
            }
        }
    }

    public void writeLine(String identificator, byte[] command_data, byte[] response_data) {
        if (_logging) {
            try {
                if (!_headerWritten) {
                    writeHeader();
                    _headerWritten = true;
                }
                _writer.append(identificator);
                _writer.append(',');
                _writer.append(HexToolkit.toString(command_data));
                _writer.append(',');
                _writer.append(HexToolkit.toString(response_data));
                _writer.append('\n');

                _writer.flush();
            } catch (IOException e) {
                System.err.println(LoggingUtils.formatDebugMessage("Unable to write line into the CSV file, something's wrooooong, panic, panic, exit."));
                e.printStackTrace(System.err);
                System.exit(1);
            }
        }
    }
}
