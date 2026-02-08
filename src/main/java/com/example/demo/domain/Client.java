package com.example.demo.domain;

public class Client {

    private String name;
    private String ip;
    private String publicIp;
    private long bytesReceived;
    private long bytesSent;
    private String connectedSince;
    private String status;

    public Client() {}
    public Client(String name, String ip, String publicIp, long bytesReceived, long bytesSent, String connectedSince, String status) {
        this.name = name;
        this.ip = ip;
        this.publicIp = publicIp;
        this.bytesReceived = bytesReceived;
        this.bytesSent = bytesSent;
        this.connectedSince = connectedSince;
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public String getIp() {
        return ip;
    }

    public String getPublicIp() {
        return publicIp;
    }

    public long getBytesReceived() {
        return bytesReceived;
    }

    public long getBytesSent() {
        return bytesSent;
    }

    public String getConnectedSince() {
        return connectedSince;
    }

    public String getStatus() {
        return status;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public void setPublicIp(String publicIp) {
        this.publicIp = publicIp;
    }

    public void setBytesReceived(long bytesReceived) {
        this.bytesReceived = bytesReceived;
    }

    public void setBytesSent(long bytesSent) {
        this.bytesSent = bytesSent;
    }

    public void setConnectedSince(String connectedSince) {
        this.connectedSince = connectedSince;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
