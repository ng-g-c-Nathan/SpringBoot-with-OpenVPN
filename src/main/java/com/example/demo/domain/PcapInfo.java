package com.example.demo.domain;

import java.time.Instant;

public class PcapInfo {
    private String name;
    private Instant lastModified;
    private long size;
    private String csvStatus;
    private String csvPath;

    public PcapInfo(String name, Instant lastModified, long size, String csvStatus, String csvPath) {
        this.name = name;
        this.lastModified = lastModified;
        this.size = size;
        this.csvStatus = csvStatus;
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

    public String getCsvStatus() {
        return csvStatus;
    }

    public void setCsvStatus(String csvStatus) {
        this.csvStatus = csvStatus;
    }

    public String getCsvPath() {
        return csvPath;
    }

    public void setCsvPath(String csvPath) {
        this.csvPath = csvPath;
    }
}
