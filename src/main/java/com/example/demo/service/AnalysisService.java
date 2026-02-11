package com.example.demo.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.io.File;
@Service
public class AnalysisService {

    @Value("${traffic.dir}")
    private String TRAFFIC_DIR;

    @Value("${python.path}")
    private String PYTHON_PATH;

    private static final String PYTHON_SCRIPT = "Controller.py";
    private static final String HISTORY_FILE = "analysis_history.json";


    public void runAnalysis(String filename) throws Exception {

        String rawPath = TRAFFIC_DIR;

        if (rawPath.endsWith("KillSwitchdaily")) {
            rawPath = rawPath.replace(
                    "KillSwitchdaily",
                    "KillSwitch" + File.separator + "daily"
            );
        }


        if (!filename.toLowerCase().endsWith(".csv")) {
            filename += ".csv";
        }

        Path csvPath = Paths.get(rawPath)
                .resolve(filename)
                .toAbsolutePath();

        if (!csvPath.toFile().exists()) {
            throw new RuntimeException("CSV no existe: " + csvPath);
        }

        Path pythonDir = Paths.get(PYTHON_PATH).toAbsolutePath();
        Path scriptPath = pythonDir.resolve(PYTHON_SCRIPT);

        if (!scriptPath.toFile().exists()) {
            throw new RuntimeException("Script no existe: " + scriptPath);
        }

        ProcessBuilder pb = new ProcessBuilder(
                "python",
                scriptPath.toString(),
                csvPath.toString()
        );

        pb.directory(pythonDir.toFile());

        // No nos importa el output
        pb.redirectErrorStream(true);

        pb.start();   // <-- NO waitFor()
    }


    public File getHistoryFile() {

        Path historyPath = Paths.get(PYTHON_PATH)
                .resolve(HISTORY_FILE)
                .toAbsolutePath();

        File f = historyPath.toFile();

        if (!f.exists()) {
            throw new RuntimeException("No existe analysis_history.json");
        }

        return f;
    }
}
