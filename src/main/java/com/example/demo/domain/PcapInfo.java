package com.example.demo.domain;

import java.time.Instant;

public class PcapInfo {
    private String name;
    private Instant lastModified;
    private long size;
    private boolean csvExists;
    private String csvPath;


    public PcapInfo(String name, Instant lastModified, long size, boolean csvExists, String csvPath) {
        this.name = name;
        this.lastModified = lastModified;
        this.size = size;
        this.csvExists = csvExists;
        this.csvPath = csvPath;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Instant getLastModified() {
        return lastModified;
    }

    public void setLastModified(Instant lastModified) {
        this.lastModified = lastModified;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public boolean isCsvExists() {
        return csvExists;
    }

    public void setCsvExists(boolean csvExists) {
        this.csvExists = csvExists;
    }

    public String getCsvPath() {
        return csvPath;
    }

    public void setCsvPath(String csvPath) {
        this.csvPath = csvPath;
    }
}
