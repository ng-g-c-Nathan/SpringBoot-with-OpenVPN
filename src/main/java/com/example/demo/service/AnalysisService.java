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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Service
public class AnalysisService {

    @Value("${traffic.dir}")
    private String TRAFFIC_DIR;

    @Value("${python.path}")
    private String PYTHON_PATH;

    private static final String PYTHON_SCRIPT = "Controller.py";
    private static final String HISTORY_FILE = "analysis_history.json";
    private static final String TRAIN_SCRIPT = "train_models.py";


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

    public String getAllModelInfo() throws Exception {
        Path modelsDir = Paths.get(PYTHON_PATH, "models").toAbsolutePath();
        File[] modelFolders = modelsDir.toFile().listFiles(File::isDirectory);

        if (modelFolders == null || modelFolders.length == 0) {
            throw new RuntimeException("No hay carpetas en models");
        }

        ObjectMapper mapper = new ObjectMapper();
        ArrayNode combined = mapper.createArrayNode();

        for (File folder : modelFolders) {
            File infoFile = new File(folder, "model_info.json");
            if (infoFile.exists()) {
                ObjectNode node = (ObjectNode) mapper.readTree(infoFile);
                node.put("folder", folder.getName()); // opcional, para identificar la carpeta
                combined.add(node);
            }
        }

        return mapper.writeValueAsString(combined);
    }

    public File getTrainingLogFile() {
        Path logPath = Paths.get(PYTHON_PATH, "models", "training_log.json").toAbsolutePath();
        File f = logPath.toFile();

        if (!f.exists()) {
            throw new RuntimeException("No existe training_log.json");
        }

        return f;
    }
    public void runTraining(String mode, String fromDate, String toDate) throws Exception {

        if (mode == null || mode.isBlank()) {
            throw new RuntimeException("mode es requerido");
        }

        Path pythonDir = Paths.get(PYTHON_PATH).toAbsolutePath();
        Path scriptPath = pythonDir.resolve(TRAIN_SCRIPT);

        if (!scriptPath.toFile().exists()) {
            throw new RuntimeException("Script no existe: " + scriptPath);
        }

        ProcessBuilder pb;

        boolean hasDates =
                fromDate != null && !fromDate.isBlank()
                        && toDate != null && !toDate.isBlank();

        if (hasDates) {

            pb = new ProcessBuilder(
                    "python",
                    scriptPath.toString(),
                    mode,
                    fromDate,
                    toDate
            );

        } else {

            pb = new ProcessBuilder(
                    "python",
                    scriptPath.toString(),
                    mode
            );
        }

        pb.directory(pythonDir.toFile());
        pb.redirectErrorStream(true);

        pb.start();
    }


}
