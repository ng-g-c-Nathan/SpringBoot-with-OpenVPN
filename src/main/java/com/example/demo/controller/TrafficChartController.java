package com.example.demo.controller;

import com.example.demo.domain.TrafficDayStats;
import com.example.demo.service.CsvTrafficIndexService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/traffic")
public class TrafficChartController {

    private final CsvTrafficIndexService service;

    public TrafficChartController(CsvTrafficIndexService service) {
        this.service = service;
    }

    @GetMapping("/last/{days}")
    public List<TrafficDayStats> lastDays(@PathVariable int days) {
        return service.lastDays(days);
    }

    @GetMapping("/range")
    public List<TrafficDayStats> range(@RequestParam LocalDate from, @RequestParam LocalDate to) {
        return service.between(from, to);
    }

    @GetMapping("/top")
    public List<TrafficDayStats> top(@RequestParam(defaultValue = "5") int limit) {
        return service.topDays(limit);
    }


}
