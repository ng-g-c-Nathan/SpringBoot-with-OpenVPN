package com.example.demo.controller;
import com.example.demo.domain.PcapInfo;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/csv_files")
public class CsvFilesController {

    @Value("${traffic.dir}")
    private String TRAFFIC_DIR;

    @Value("${reg.script.path}")
    private String REG_DIR;


    private Path findGeneratedCsv(Path dir, String pcapFilename) throws IOException {

        String base = pcapFilename.replaceFirst("\\.pcap$", "");

        try (Stream<Path> s = Files.list(dir)) {
            return s
                    .filter(p -> p.getFileName().toString().startsWith(base))
                    .filter(p -> p.getFileName().toString().endsWith(".csv"))
                    .findFirst()
                    .orElse(null);
        }
    }


    private double extractMinutesFromFilename(String filename) {

        Pattern p = Pattern.compile("\\((\\d+(?:\\.\\d+)?)_minutes\\)");
        Matcher m = p.matcher(filename);

        if (m.find()) {
            return Double.parseDouble(m.group(1));
        }

        return -1;
    }

    private boolean existsGeneratedCsv(Path dir, String pcapName) throws IOException {

        String base = pcapName.replaceAll("\\.pcap$", "");

        try (Stream<Path> s = Files.list(dir)) {
            return s.anyMatch(p ->
                    p.getFileName().toString().startsWith(base)
                            && p.getFileName().toString().endsWith(".csv")
            );
        }
    }

    private String resolveCsvStatus(Path pcapPath, Path csvDir) throws IOException {

        Path csv = findGeneratedCsv(csvDir, pcapPath.getFileName().toString());

        if (csv != null) {
            return "true";
        }

        String filename = pcapPath.getFileName().toString();

        double minutes = extractMinutesFromFilename(filename);

        if (minutes <= 0) {
            return "pending";
        }

        long tripleMillis = (long) (minutes * 3 * 60_000);

        long lastModified = Files.getLastModifiedTime(pcapPath).toMillis();
        long now = System.currentTimeMillis();

        if (now - lastModified > tripleMillis) {
            return "false";
        }

        return "pending";
    }



    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<PcapInfo>> listPcapFiles() {
        Path dirPath = Paths.get(TRAFFIC_DIR);
        Path csvDirPath = Paths.get(TRAFFIC_DIR);

        if (!Files.exists(dirPath) || !Files.isDirectory(dirPath)) {
            return ResponseEntity.status(404).build();
        }

        try {
            List<PcapInfo> pcapList = Files.list(dirPath)
                    .filter(path -> path.toString().endsWith(".pcap"))
                    .sorted((a, b) -> {
                        try {
                            return Long.compare(Files.getLastModifiedTime(b).toMillis(),
                                    Files.getLastModifiedTime(a).toMillis());
                        } catch (IOException e) {
                            return 0;
                        }
                    })
                    .map(path -> {
                        try {
                            File file = path.toFile();

                            Path csv = findGeneratedCsv(csvDirPath, file.getName());
                            String status = resolveCsvStatus(path, csvDirPath);

                            String csvPath = csv != null
                                    ? csv.getFileName().toString()
                                    : null;

                            return new PcapInfo(
                                    file.getName(),
                                    Files.getLastModifiedTime(path).toInstant(),
                                    file.length(),
                                    status,
                                    csvPath
                            );

                        } catch (Exception e) {
                            return null;
                        }
                    })


                    .filter(info -> info != null)
                    .toList();

            return ResponseEntity.ok(pcapList);

        } catch (IOException e) {
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping("/reparar/{filename:.+}")
    public ResponseEntity<?> reparar(@PathVariable String filename) {

        Path pcapPath = Paths.get(TRAFFIC_DIR).resolve(filename);

        if (!Files.exists(pcapPath)) {
            return ResponseEntity.notFound().build();
        }

        try {

            String serverIp = "10.0.0.8";
            ProcessBuilder pb = new ProcessBuilder(
                    "python3",
                    REG_DIR,
                    pcapPath.toString(),
                    serverIp
            );

            pb.redirectErrorStream(true);
            pb.start();

            // queda en pending
            return ResponseEntity.accepted().build();

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error ejecutando Pruebas.py");
        }
    }

}
