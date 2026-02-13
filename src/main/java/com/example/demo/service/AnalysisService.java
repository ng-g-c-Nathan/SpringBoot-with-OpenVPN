package com.example.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Servicio AnalysisService
 * * Gestiona la interoperabilidad entre la API de Spring y el motor de IA en Python.
 * Se encarga de la orquestación de scripts externos para el análisis de tráfico (scoring)
 * y el re-entrenamiento de modelos, además de consolidar la información técnica de los modelos.
 */
@Service
public class AnalysisService {

    // --- CONFIGURACIÓN DE RUTAS ---

    @Value("${traffic.dir}")
    private String TRAFFIC_DIR;

    @Value("${python.path}")
    private String PYTHON_PATH;

    /** Nombres de los scripts y archivos de datos compartidos con el motor Python */
    private static final String PYTHON_SCRIPT = "Controller.py";
    private static final String HISTORY_FILE = "analysis_history.json";
    private static final String TRAIN_SCRIPT = "train_models.py";

    // --- LÓGICA DE ANÁLISIS (SCORING) ---

    /**
     * Ejecuta el script de detección de anomalías sobre un archivo CSV.
     * * Realiza una normalización de rutas de sistema y lanza el proceso en segundo plano.
     * * @param filename Nombre del archivo CSV a procesar.
     * @throws Exception Si los archivos necesarios no existen o falla el arranque del proceso.
     */
    public void runAnalysis(String filename) throws Exception {

        // --- NORMALIZACIÓN DE RUTA ---
        String rawPath = TRAFFIC_DIR;

        // Corrección de path específica para la estructura de directorios del servidor
        if (rawPath.endsWith("KillSwitchdaily")) {
            rawPath = rawPath.replace(
                    "KillSwitchdaily",
                    "KillSwitch" + File.separator + "daily"
            );
        }

        // Aseguramos la extensión correcta del archivo
        if (!filename.toLowerCase().endsWith(".csv")) {
            filename += ".csv";
        }

        Path csvPath = Paths.get(rawPath).resolve(filename).toAbsolutePath();

        if (!csvPath.toFile().exists()) {
            throw new RuntimeException("CSV no existe en la ruta: " + csvPath);
        }

        // --- PREPARACIÓN DEL SCRIPT PYTHON ---
        Path pythonDir = Paths.get(PYTHON_PATH).toAbsolutePath();
        Path scriptPath = pythonDir.resolve(PYTHON_SCRIPT);

        if (!scriptPath.toFile().exists()) {
            throw new RuntimeException("Script de control no encontrado: " + scriptPath);
        }

        ProcessBuilder pb = new ProcessBuilder(
                "python",
                scriptPath.toString(),
                csvPath.toString()
        );

        pb.directory(pythonDir.toFile());
        pb.redirectErrorStream(true);

        // Se inicia el proceso en segundo plano (Fire and Forget)
        pb.start();
    }

    // --- GESTIÓN DE ARCHIVOS Y METADATOS ---

    /**
     * Recupera el archivo de historial de análisis generado por el motor de IA.
     * @return Archivo File apuntando a analysis_history.json.
     */
    public File getHistoryFile() {
        Path historyPath = Paths.get(PYTHON_PATH).resolve(HISTORY_FILE).toAbsolutePath();
        File f = historyPath.toFile();

        if (!f.exists()) {
            throw new RuntimeException("Archivo de historial no disponible.");
        }
        return f;
    }

    /**
     * Recopila la información técnica de todos los modelos de IA disponibles.
     * * Escanea las subcarpetas del directorio de modelos y combina sus archivos 'model_info.json'.
     * * @return String JSON consolidado con los metadatos de todos los modelos.
     * @throws Exception Si ocurre un error en la lectura o parseo del JSON.
     */
    public String getAllModelInfo() throws Exception {
        Path modelsDir = Paths.get(PYTHON_PATH, "models").toAbsolutePath();
        File[] modelFolders = modelsDir.toFile().listFiles(File::isDirectory);

        if (modelFolders == null || modelFolders.length == 0) {
            throw new RuntimeException("No se encontraron modelos registrados.");
        }

        ObjectMapper mapper = new ObjectMapper();
        ArrayNode combined = mapper.createArrayNode();

        for (File folder : modelFolders) {
            File infoFile = new File(folder, "model_info.json");
            if (infoFile.exists()) {
                ObjectNode node = (ObjectNode) mapper.readTree(infoFile);
                node.put("folder", folder.getName());
                combined.add(node);
            }
        }

        return mapper.writeValueAsString(combined);
    }

    /**
     * Obtiene el archivo log con el seguimiento del último entrenamiento.
     * @return Archivo File apuntando a training_log.json.
     */
    public File getTrainingLogFile() {
        Path logPath = Paths.get(PYTHON_PATH, "models", "training_log.json").toAbsolutePath();
        File f = logPath.toFile();

        if (!f.exists()) {
            throw new RuntimeException("Log de entrenamiento no encontrado.");
        }
        return f;
    }

    // --- LÓGICA DE ENTRENAMIENTO ---

    /**
     * Dispara el script de entrenamiento de modelos.
     * * Permite la ejecución con o sin rango de fechas según la disponibilidad de parámetros.
     * * @param mode Modo de entrenamiento
     * @param fromDate Fecha inicial del set de datos (opcional).
     * @param toDate Fecha final del set de datos (opcional).
     * @throws Exception Si falla la ejecución del script de Python.
     */
    public void runTraining(String mode, String fromDate, String toDate) throws Exception {

        if (mode == null || mode.isBlank()) {
            throw new RuntimeException("El modo de entrenamiento es obligatorio.");
        }

        Path pythonDir = Paths.get(PYTHON_PATH).toAbsolutePath();
        Path scriptPath = pythonDir.resolve(TRAIN_SCRIPT);

        if (!scriptPath.toFile().exists()) {
            throw new RuntimeException("Script de entrenamiento no encontrado.");
        }

        ProcessBuilder pb;
        boolean hasDates = fromDate != null && !fromDate.isBlank()
                && toDate != null && !toDate.isBlank();

        // Configuración dinámica de argumentos de línea de comandos
        if (hasDates) {
            pb = new ProcessBuilder("python", scriptPath.toString(), mode, fromDate, toDate);
        } else {
            pb = new ProcessBuilder("python", scriptPath.toString(), mode);
        }

        pb.directory(pythonDir.toFile());
        pb.redirectErrorStream(true);

        pb.start();
    }
}