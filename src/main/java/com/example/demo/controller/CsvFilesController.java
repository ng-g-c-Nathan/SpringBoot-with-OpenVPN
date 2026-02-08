package com.example.demo.controller;
import com.example.demo.domain.PcapInfo;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/csv_files")
public class CsvFilesController {

    @Value("${traffic.dir}")
    private String TRAFFIC_DIR;


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
                            String csvFileName = file.getName().replaceAll("\\.pcap$", ".csv");
                            Path csvPath = csvDirPath.resolve(csvFileName);
                            boolean csvExists = Files.exists(csvPath);

                            return new PcapInfo(
                                    file.getName(),
                                    Files.getLastModifiedTime(path).toInstant(),
                                    file.length(),
                                    csvExists,
                                    csvExists ? csvPath.toString() : null
                            );
                        } catch (IOException e) {
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

}
