package com.Fenrro.JESP_Core.service;

import com.Fenrro.JESP_Core.entity.User;
import com.Fenrro.JESP_Core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${jesp.admin-password:}")
    private String adminPassword;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.count() == 0) {
            String password = (adminPassword == null || adminPassword.isBlank())
                    ? UUID.randomUUID().toString().substring(0, 12)
                    : adminPassword;
            userRepository.save(new User("admin", passwordEncoder.encode(password), User.Role.ADMIN));
            if (adminPassword == null || adminPassword.isBlank()) {
                log.warn("Administrador creado con contraseña aleatoria: {}", password);
                log.warn("Configura JESP_ADMIN_PASSWORD para elegir tu propia contraseña.");
            } else {
                log.info("Administrador 'admin' creado con la contraseña de JESP_ADMIN_PASSWORD.");
            }
        }
    }

    public User getByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(username));
    }

    public boolean passwordMatches(User user, String rawPassword) {
        return passwordEncoder.matches(rawPassword, user.getPasswordHash());
    }

    public List<User> list() {
        return userRepository.findAll();
    }

    public User create(String username, String rawPassword, User.Role role) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("El usuario ya existe");
        }
        return userRepository.save(new User(username, passwordEncoder.encode(rawPassword), role));
    }

    public void delete(Long id, String requesterUsername) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        if (user.getUsername().equals(requesterUsername)) {
            throw new IllegalArgumentException("No puedes eliminar tu propio usuario");
        }
        userRepository.delete(user);
    }

    public void changePassword(Long id, String requesterUsername, boolean requesterIsAdmin,
                               String newPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        if (!requesterIsAdmin && !user.getUsername().equals(requesterUsername)) {
            throw new IllegalArgumentException("Solo puedes cambiar tu propia contraseña");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
