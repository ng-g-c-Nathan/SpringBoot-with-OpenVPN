package com.example.demo.domain;

import java.time.Instant;

public class CsvFile {
    private String name;
    private Instant lastModified;
    private long size;
    private boolean csvExists;

    public CsvFile(String name, Instant lastModified, long size, boolean csvExists) {
        this.name = name;
        this.lastModified = lastModified;
        this.size = size;
        this.csvExists = csvExists;
    }

    public String getName() {
        return name;
    }

    public Instant getLastModified() {
        return lastModified;
    }

    public long getSize() {
        return size;
    }

    public boolean isCsvExists() {
        return csvExists;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setLastModified(Instant lastModified) {
        this.lastModified = lastModified;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public void setCsvExists(boolean csvExists) {
        this.csvExists = csvExists;
    }
}
