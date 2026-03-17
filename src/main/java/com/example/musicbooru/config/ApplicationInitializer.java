package com.example.musicbooru.config;

import com.example.musicbooru.model.Role;
import com.example.musicbooru.model.User;
import com.example.musicbooru.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static com.example.musicbooru.util.Commons.*;

@Component
@RequiredArgsConstructor
public class ApplicationInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username}")
    private String adminUsername;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Override
    public void run(ApplicationArguments args) {
        createDefaultAdminAccount();
        createAppDirectories();
    }

    private void createDefaultAdminAccount() {
        if (!userRepository.existsByUsername(adminUsername)) {
            User admin = User.builder()
                    .username(adminUsername)
                    .password(passwordEncoder.encode(adminPassword))
                    .role(Role.ADMIN)
                    .build();

            userRepository.save(admin);
        }
    }

    private void createAppDirectories() {
        final List<Path> APP_DIRECTORIES = List.of(
                Paths.get(LIBRARY),
                Paths.get(ARTWORK),
                Paths.get(ICON)
        );

        APP_DIRECTORIES.forEach(dir -> {
            try {
                Files.createDirectories(dir);
            } catch (IOException e) {
                throw new RuntimeException("Could not create directory: " + dir, e);
            }
        });
    }
}
