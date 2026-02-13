package com.example.demo.domain;

/**
 * Entidad de Dominio: User
 * * Representa a un usuario registrado con acceso al sistema de gestión VPN.
 * Esta clase define el perfil de identidad, las credenciales de acceso y el nivel
 * de privilegios (rol) que determina qué acciones puede realizar el usuario dentro de la API.
 */
public class User {

    // --- INFORMACIÓN DE PERFIL ---

    /** Nombre completo o alias del usuario */
    private String name;

    /** Correo electrónico único utilizado como identificador de inicio de sesión (Login ID) */
    private String email;

    // --- SEGURIDAD Y PERMISOS ---

    /** * Rol asignado al usuario
     * Determina el nivel de autorización para ejecutar comandos o descargar reportes.
     */
    private String role;

    /** Contraseña almacenada */
    private String password;

    // --- CONSTRUCTORES ---

    /** Constructor vacío requerido para la persistencia y serialización JSON */
    public User() {}

    /**
     * Constructor completo para la creación de nuevos perfiles de usuario.
     * @param name Nombre del usuario.
     * @param email Correo electrónico.
     * @param role Perfil de permisos.
     * @param password Credencial de acceso.
     */
    public User(String name, String email, String role, String password) {
        this.name = name;
        this.email = email;
        this.role = role;
        this.password = password;
    }

    // --- GETTERS Y SETTERS ---

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}