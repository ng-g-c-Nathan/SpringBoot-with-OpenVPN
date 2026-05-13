package com.example.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Servicio responsable de coordinar las operaciones de análisis y entrenamiento
 * de modelos de Machine Learning a través de una API externa escrita en Python.
 *
 * <p>Este servicio forma parte de la capa de servicios del backend desarrollado
 * con {@link org.springframework.stereotype.Service} en un proyecto basado en
 * {@code Spring Boot}. Su función principal es delegar el procesamiento pesado
 * de Machine Learning a un microservicio Python (Flask), evitando ejecutar
 * procesos locales dentro de la aplicación Java.</p>
 *
 * <p>Las responsabilidades principales de este servicio son:</p>
 *
 * <ul>
 *     <li>Enviar solicitudes de análisis (scoring) al microservicio Python.</li>
 *     <li>Solicitar entrenamiento de modelos de Machine Learning.</li>
 *     <li>Leer archivos generados por el sistema de análisis (historial y logs).</li>
 *     <li>Consultar metadatos de modelos disponibles.</li>
 * </ul>
 *
 * <p>La comunicación entre Java y Python se realiza mediante HTTP utilizando
 * {@link RestTemplate}, mientras que los archivos de resultados se comparten
 * mediante un volumen compartido entre contenedores.</p>
 *
 * <p>Arquitectura simplificada:</p>
 *
 * <pre>
 * Spring Boot (Java)
 *        |
 *        | HTTP REST
 *        v
 * Python Flask API (python-scorer)
 *        |
 *        | Ejecuta scripts de ML
 *        v
 * Modelos y resultados en volumen compartido
 * </pre>
 *
 * @author
 */
@Service
public class AnalysisService {

    /**
     * Directorio donde se almacenan los archivos de tráfico CSV.
     * Esta ruta se obtiene desde la configuración del archivo
     * {@code application.properties}.
     */
    @Value("${traffic.dir}")
    private String TRAFFIC_DIR;

    /**
     * URL base de la API Flask que ejecuta los modelos de Machine Learning.
     *
     * <p>Normalmente esta URL apunta al contenedor Docker que ejecuta
     * el servicio Python. Si no se define una variable de entorno,
     * se utiliza el valor por defecto:</p>
     *
     * <pre>
     * http://python-scorer:5000
     * </pre>
     */
    @Value("${python.api.url:http://python-scorer:5000}")
    private String PYTHON_API_URL;

    /**
     * Ruta del sistema de archivos compartido entre contenedores
     * donde se almacenan resultados generados por el servicio Python.
     *
     * <p>Esta ruta permite a la aplicación Java acceder a archivos
     * como:</p>
     *
     * <ul>
     *     <li>Historial de análisis</li>
     *     <li>Información de modelos</li>
     *     <li>Logs de entrenamiento</li>
     * </ul>
     */
    @Value("${python.path:/app}")
    private String PYTHON_PATH;

    /**
     * Nombre del archivo que almacena el historial de análisis realizados.
     */
    private static final String HISTORY_FILE = "analysis_history.json";

    /**
     * Cliente HTTP utilizado para comunicarse con la API Flask.
     */
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Utilidad de Jackson para serialización y deserialización JSON.
     */
    private final ObjectMapper mapper = new ObjectMapper();

    // --- ANÁLISIS (SCORING) ---

    /**
     * Solicita al microservicio Python que ejecute un análisis de tráfico
     * utilizando los modelos de Machine Learning disponibles.
     *
     * <p>Este método envía una petición HTTP POST al endpoint
     * {@code /score} de la API Flask. El servidor Python se encarga de
     * ejecutar el script de análisis en segundo plano.</p>
     *
     * <p>La API Python responde inmediatamente con un estado HTTP 202
     * (Accepted), mientras que el procesamiento real se ejecuta de forma
     * asíncrona.</p>
     *
     * @param filename nombre del archivo CSV que contiene el tráfico
     *                 a analizar. Si no incluye la extensión ".csv",
     *                 se agrega automáticamente.
     *
     * @param range rango de análisis solicitado. Puede ser por ejemplo:
     *              <ul>
     *                  <li>{@code global}</li>
     *                  <li>{@code daily}</li>
     *                  <li>otros rangos definidos en el sistema</li>
     *              </ul>
     *
     * @throws Exception si ocurre un error al comunicarse con la API Flask
     */
    public void runAnalysis(String filename, String range) throws Exception {

        if (!filename.toLowerCase().endsWith(".csv")) {
            filename += ".csv";
        }

        Map<String, String> body = new HashMap<>();
        body.put("csv_file", filename);
        body.put("range", range != null ? range : "global");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

        try {
            restTemplate.postForEntity(PYTHON_API_URL + "/score", entity, String.class);
        } catch (Exception e) {
            throw new RuntimeException("Error llamando a Flask /score: " + e.getMessage());
        }
    }

    // --- HISTORIAL ---

    /**
     * Obtiene el archivo que contiene el historial de análisis realizados.
     *
     * <p>El historial es generado por el microservicio Python y almacenado
     * en el volumen compartido entre contenedores.</p>
     *
     * @return archivo {@link File} que representa el historial de análisis
     *
     * @throws RuntimeException si el archivo de historial no existe
     */
    public File getHistoryFile() {
        Path historyPath = Paths.get(PYTHON_PATH).resolve(HISTORY_FILE).toAbsolutePath();
        File f = historyPath.toFile();

        if (!f.exists()) {
            throw new RuntimeException("Archivo de historial no disponible.");
        }
        return f;
    }

    // --- MODELOS INFO ---

    /**
     * Consulta la información de todos los modelos de Machine Learning
     * registrados en el sistema.
     *
     * <p>La información se obtiene llamando al endpoint {@code /models_info}
     * del servicio Flask.</p>
     *
     * <p>Los datos pueden incluir:</p>
     *
     * <ul>
     *     <li>Nombre del modelo</li>
     *     <li>Fecha de entrenamiento</li>
     *     <li>Métricas de desempeño</li>
     *     <li>Configuración utilizada</li>
     * </ul>
     *
     * @return cadena JSON con la información de los modelos
     *
     * @throws Exception si ocurre un error durante la llamada HTTP
     */
    public String getAllModelInfo() throws Exception {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    PYTHON_API_URL + "/models_info", String.class);
            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("Error llamando a Flask /models_info: " + e.getMessage());
        }
    }

    // --- TRAINING LOG ---

    /**
     * Obtiene el archivo de log correspondiente al proceso de entrenamiento
     * de modelos de Machine Learning.
     *
     * <p>Este archivo es generado por el sistema Python durante la ejecución
     * del script de entrenamiento.</p>
     *
     * @return archivo {@link File} con el log de entrenamiento
     *
     * @throws RuntimeException si el archivo no existe
     */
    public File getTrainingLogFile() {
        Path logPath = Paths.get(PYTHON_PATH, "models", "training_log.json").toAbsolutePath();
        File f = logPath.toFile();

        if (!f.exists()) {
            throw new RuntimeException("Log de entrenamiento no encontrado.");
        }
        return f;
    }

    // --- ENTRENAMIENTO ---

    /**
     * Solicita al microservicio Python que inicie un proceso de
     * entrenamiento de modelos de Machine Learning.
     *
     * <p>Este método envía una petición HTTP POST al endpoint
     * {@code /train} del servicio Flask.</p>
     *
     * <p>El proceso de entrenamiento se ejecuta de forma asíncrona,
     * por lo que la API responde inmediatamente con estado HTTP 202.</p>
     *
     * @param mode modo de entrenamiento. Puede representar diferentes
     *             estrategias de entrenamiento (por ejemplo: full,
     *             incremental, por rango de fechas, etc.).
     *
     * @param fromDate fecha inicial del rango de entrenamiento
     *                 (opcional).
     *
     * @param toDate fecha final del rango de entrenamiento
     *               (opcional).
     *
     * @throws Exception si ocurre un error al comunicarse con la API Python
     */
    public void runTraining(String mode, String fromDate, String toDate) throws Exception {

        if (mode == null || mode.isBlank()) {
            throw new RuntimeException("El modo de entrenamiento es obligatorio.");
        }

        Map<String, String> body = new HashMap<>();
        body.put("mode", mode);
        if (fromDate != null && !fromDate.isBlank()) body.put("fromDate", fromDate);
        if (toDate   != null && !toDate.isBlank())   body.put("toDate",   toDate);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

        try {
            restTemplate.postForEntity(PYTHON_API_URL + "/train", entity, String.class);
        } catch (Exception e) {
            throw new RuntimeException("Error llamando a Flask /train: " + e.getMessage());
        }
    }
}