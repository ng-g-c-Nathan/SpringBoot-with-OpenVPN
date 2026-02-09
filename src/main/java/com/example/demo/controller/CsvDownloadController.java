package com.example.demo.controller;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.beans.factory.annotation.Value;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/csv_files")
public class CsvDownloadController {


    @Value("${traffic.dir}")
    private String TRAFFIC_DIR;

    public static class CsvRequest {
        private String CSVFILE;

        public String getCSVFILE() {
            return CSVFILE;
        }

        public void setCSVFILE(String CSVFILE) {
            this.CSVFILE = CSVFILE;
        }
    }

    @PostMapping("/download")
    public ResponseEntity<StreamingResponseBody> downloadCsv(
            @RequestBody(required = false) CsvRequest request
    ) {

        if (request == null || request.getCSVFILE() == null || request.getCSVFILE().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        String filename = request.getCSVFILE();

        // Protección básica contra path traversal
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            return ResponseEntity.badRequest().build();
        }

        Path filePath = Paths.get(TRAFFIC_DIR).resolve(filename);

        if (!Files.exists(filePath)) {
            return ResponseEntity.notFound().build();
        }

        StreamingResponseBody stream = outputStream -> {

            // buffer grande para mejor throughput
            byte[] buffer = new byte[64 * 1024];

            try (InputStream in = Files.newInputStream(filePath)) {

                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    outputStream.flush();
                }

            }
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(stream);
    }

}
