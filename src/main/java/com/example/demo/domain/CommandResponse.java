package com.example.demo.domain;

public class CommandResponse {

    private String status;
    private String message;
    private String output;

    public CommandResponse() {
    }

    public CommandResponse(String status, String message, String output) {
        this.status = status;
        this.message = message;
        this.output = output;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }
}
