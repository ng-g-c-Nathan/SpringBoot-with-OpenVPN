package com.example.demo.domain;

import java.time.LocalDate;

public class TrafficDayStats {

    private LocalDate date;
    private double totalInput;
    private double totalOutput;
    private int captures;

    public TrafficDayStats(LocalDate date, double totalInput, double totalOutput, int captures) {
        this.date = date;
        this.totalInput = totalInput;
        this.totalOutput = totalOutput;
        this.captures = captures;
    }

    public LocalDate getDate() {
        return date;
    }

    public double getTotalInput() {
        return totalInput;
    }

    public double getTotalOutput() {
        return totalOutput;
    }

    public int getCaptures() {
        return captures;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public void setTotalInput(double totalInput) {
        this.totalInput = totalInput;
    }

    public void setTotalOutput(double totalOutput) {
        this.totalOutput = totalOutput;
    }

    public void setCaptures(int captures) {
        this.captures = captures;
    }
}
