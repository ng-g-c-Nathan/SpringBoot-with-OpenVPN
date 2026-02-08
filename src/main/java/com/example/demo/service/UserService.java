package com.example.demo.service;

import com.example.demo.domain.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

@Service
public class UserService {
    @Value("${users.file}")
    private String usersFilePath;

    private final ObjectMapper mapper = new ObjectMapper();
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    private File getFile() throws IOException {

        File file = new File(usersFilePath);

        if (!file.exists()) {
            file.getParentFile().mkdirs();
            mapper.writeValue(file, new ArrayList<User>());
        }

        return file;
    }

    public synchronized void register(String name,
                                      String email,
                                      String role,
                                      String rawPassword) throws IOException {

        File file = getFile();

        List<User> users = mapper.readValue(
                file,
                new TypeReference<List<User>>() {
                }
        );

        boolean exists = users.stream()
                .anyMatch(u -> u.getEmail().equalsIgnoreCase(email));

        if (exists) {
            throw new IllegalStateException("Email already registered");
        }

        String hash = encoder.encode(rawPassword);

        users.add(new User(
                name,
                email,
                role,
                hash
        ));

        mapper.writeValue(file, users);
    }

    public boolean login(String email, String rawPassword) throws IOException {

        File file = getFile();

        List<User> users = mapper.readValue(
                file,
                new TypeReference<List<User>>() {
                }
        );

        return users.stream()
                .filter(u -> u.getEmail().equalsIgnoreCase(email))
                .findFirst()
                .map(u -> encoder.matches(rawPassword, u.getPassword()))
                .orElse(false);
    }


}
