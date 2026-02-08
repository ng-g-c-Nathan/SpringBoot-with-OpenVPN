package com.example.demo.service;

import com.example.demo.domain.TrafficDayStats;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.time.LocalDate;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Service
public class CsvTrafficIndexService {

    private final TreeMap<LocalDate, TrafficDayStats> index = new TreeMap<>();

    @Value("${traffic.dir}")
    private String trafficDir;

    private static final Pattern FILE_PATTERN =
            Pattern.compile(
                    "traffic_(\\d{4}-\\d{2}-\\d{2})_.*_\\(([^_]+)_input\\)_\\(([^_]+)_output\\)\\.csv"
            );

    @PostConstruct
    public void buildIndex() throws IOException {

        Path dir = Paths.get(trafficDir);

        if (!Files.exists(dir)) return;

        try (Stream<Path> stream = Files.list(dir)) {

            stream
                    .filter(p -> p.getFileName().toString().endsWith(".csv"))
                    .forEach(this::processFile);
        }
    }

    private void processFile(Path path) {

        String name = path.getFileName().toString();

        Matcher m = FILE_PATTERN.matcher(name);

        if (!m.matches()) return;

        LocalDate date = LocalDate.parse(m.group(1));
        double input = Double.parseDouble(m.group(2));
        double output = Double.parseDouble(m.group(3));

        index.compute(date, (k, v) -> {
            if (v == null)
                return new TrafficDayStats(k, input, output, 1);

            v.setTotalInput(v.getTotalInput() + input);
            v.setTotalOutput(v.getTotalOutput() + output);
            v.setCaptures(v.getCaptures() + 1);
            return v;
        });
    }

    // query últimos N días
    public List<TrafficDayStats> lastDays(int days) {

        if (index.isEmpty()) return List.of();

        LocalDate to = index.lastKey();
        LocalDate from = to.minusDays(days - 1);

        return index.subMap(from, true, to, true)
                .values()
                .stream()
                .toList();
    }

    //Rango de fechas
    public List<TrafficDayStats> between(LocalDate from, LocalDate to) {

        if (index.isEmpty()) return List.of();

        return index.subMap(from, true, to, true)
                .values()
                .stream()
                .toList();
    }

    public List<TrafficDayStats> topDays(int limit) {

        PriorityQueue<TrafficDayStats> pq =
                new PriorityQueue<>(
                        Comparator.comparingDouble(
                                (TrafficDayStats d) ->
                                        d.getTotalInput() + d.getTotalOutput()
                        ).reversed()
                );

        pq.addAll(index.values());

        List<TrafficDayStats> result = new ArrayList<>();

        for (int i = 0; i < limit && !pq.isEmpty(); i++) {
            result.add(pq.poll());
        }

        return result;
    }

}
