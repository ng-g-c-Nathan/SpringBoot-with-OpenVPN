package com.example.demo.service;

import com.example.demo.domain.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio UserService
 * * Gestiona el ciclo de vida de los usuarios y la seguridad de acceso.
 * Utiliza un sistema de persistencia basado en archivos JSON y aplica hashing
 * mediante BCrypt para asegurar que las contraseñas nunca se almacenen en texto plano.
 */
@Service
public class UserService {

    @Value("${users.file}")
    private String usersFilePath;

    /** Mapper para la serialización/deserialización del archivo JSON de usuarios */
    private final ObjectMapper mapper = new ObjectMapper();

    /** Codificador para el hashing seguro de contraseñas */
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    // --- GESTIÓN DE PERSISTENCIA ---

    /**
     * Obtiene una referencia al archivo físico de usuarios.
     * * Si el archivo o los directorios no existen, los crea e inicializa una lista vacía.
     * @return File referencia al archivo JSON.
     * @throws IOException Si ocurre un error al crear el archivo o directorios.
     */
    private File getFile() throws IOException {
        File file = new File(usersFilePath);

        if (!file.exists()) {
            if (file.getParentFile() != null) {
                file.getParentFile().mkdirs();
            }
            mapper.writeValue(file, new ArrayList<User>());
        }
        return file;
    }

    // --- LÓGICA DE NEGOCIO ---

    /**
     * Registra un nuevo usuario en el sistema de forma segura.
     * * El método es 'synchronized' para prevenir condiciones de carrera al escribir en el archivo.
     * * Aplica hashing a la contraseña antes de persistirla.
     *
     * @param name Nombre del usuario.
     * @param email Identificador único (correo).
     * @param role Rol asignado (ADMIN, etc.).
     * @param rawPassword Contraseña en texto plano suministrada por el usuario.
     * @throws IOException Si falla la lectura/escritura del archivo.
     * @throws IllegalStateException Si el email ya se encuentra registrado.
     */
    public synchronized void register(String name,
                                      String email,
                                      String role,
                                      String rawPassword) throws IOException {

        File file = getFile();
        List<User> users = mapper.readValue(file, new TypeReference<List<User>>() {});

        // Validación de unicidad
        boolean exists = users.stream()
                .anyMatch(u -> u.getEmail().equalsIgnoreCase(email));

        if (exists) {
            throw new IllegalStateException("El correo electrónico ya está registrado.");
        }

        // --- SEGURIDAD: Hashing ---
        String hash = encoder.encode(rawPassword);

        users.add(new User(name, email, role, hash));
        mapper.writeValue(file, users);
    }

    /**
     * Valida las credenciales de un usuario para el inicio de sesión.
     * * Compara el hash almacenado con la contraseña suministrada usando el algoritmo BCrypt.
     *
     * @param email Email del usuario.
     * @param rawPassword Contraseña a validar.
     * @return true si las credenciales son válidas, false en caso contrario.
     * @throws IOException Si ocurre un error al consultar el repositorio de usuarios.
     */
    public boolean login(String email, String rawPassword) throws IOException {
        File file = getFile();
        List<User> users = mapper.readValue(file, new TypeReference<List<User>>() {});

        return users.stream()
                .filter(u -> u.getEmail().equalsIgnoreCase(email))
                .findFirst()
                .map(u -> encoder.matches(rawPassword, u.getPassword())) // Verificación segura
                .orElse(false);
    }
}