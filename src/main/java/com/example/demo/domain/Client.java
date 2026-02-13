package com.example.demo.domain;

/**
 * Entidad de Dominio: Client
 * * Representa a un usuario o dispositivo conectado actualmente al servidor VPN.
 * Esta clase encapsula tanto la identidad de red del cliente como sus métricas
 * de consumo de ancho de banda y tiempos de sesión.
 */
public class Client {

    // --- ATRIBUTOS DE IDENTIDAD Y RED ---

    /** Nombre identificador del cliente */
    private String name;

    /** Dirección IP local asignada por el túnel VPN */
    private String ip;

    /** Dirección IP pública desde la cual el cliente se conecta originalmente */
    private String publicIp;

    // --- MÉTRICAS DE TRÁFICO (EN BYTES) ---

    /** Cantidad total de datos recibidos por el servidor desde este cliente */
    private long bytesReceived;

    /** Cantidad total de datos enviados por el servidor hacia este cliente */
    private long bytesSent;

    // --- METADATOS DE CONEXIÓN ---

    /** Fecha y hora exacta en la que se estableció la conexión (formato string de log) */
    private String connectedSince;

    /** Estado operativo actual del cliente (ej: 'online', 'active', 'offline') */
    private String status;

    // --- CONSTRUCTORES ---

    /** Constructor vacío requerido por frameworks de serialización (Jackson) */
    public Client() {}

    /**
     * Constructor completo para instanciación directa desde el parser de logs.
     * * @param name Nombre del cliente.
     * @param ip IP virtual del túnel.
     * @param publicIp IP real de origen.
     * @param bytesReceived Acumulado de entrada.
     * @param bytesSent Acumulado de salida.
     * @param connectedSince Timestamp de inicio de sesión.
     * @param status Estado de conexión.
     */
    public Client(String name, String ip, String publicIp, long bytesReceived, long bytesSent, String connectedSince, String status) {
        this.name = name;
        this.ip = ip;
        this.publicIp = publicIp;
        this.bytesReceived = bytesReceived;
        this.bytesSent = bytesSent;
        this.connectedSince = connectedSince;
        this.status = status;
    }

    // --- GETTERS Y SETTERS ---

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }

    public String getPublicIp() { return publicIp; }
    public void setPublicIp(String publicIp) { this.publicIp = publicIp; }

    public long getBytesReceived() { return bytesReceived; }
    public void setBytesReceived(long bytesReceived) { this.bytesReceived = bytesReceived; }

    public long getBytesSent() { return bytesSent; }
    public void setBytesSent(long bytesSent) { this.bytesSent = bytesSent; }

    public String getConnectedSince() { return connectedSince; }
    public void setConnectedSince(String connectedSince) { this.connectedSince = connectedSince; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}