package com.Fenrro.JESP_Core.controller;

import com.Fenrro.JESP_Core.entity.User;
import com.Fenrro.JESP_Core.service.UserService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    public record CreateUserRequest(@NotBlank @Size(min = 3, max = 32) String username,
                                    @NotBlank @Size(min = 6, max = 72) String password,
                                    User.Role role) {}

    public record ChangePasswordRequest(@NotBlank @Size(min = 6, max = 72) String newPassword) {}

    public record UserDto(Long id, String username, String role, boolean enabled) {
        static UserDto from(User user) {
            return new UserDto(user.getId(), user.getUsername(),
                    user.getRole().name(), user.isEnabled());
        }
    }

    @GetMapping
    public List<UserDto> list() {
        return userService.list().stream().map(UserDto::from).toList();
    }

    @PostMapping
    public UserDto create(@RequestBody CreateUserRequest request) {
        User.Role role = request.role() == null ? User.Role.USER : request.role();
        return UserDto.from(userService.create(request.username(), request.password(), role));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication auth) {
        userService.delete(id, auth.getName());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/password")
    public ResponseEntity<Map<String, String>> changePassword(
            @PathVariable Long id, @RequestBody ChangePasswordRequest request, Authentication auth) {
        try {
            boolean isAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            userService.changePassword(id, auth.getName(), isAdmin, request.newPassword());
            return ResponseEntity.ok(Map.of("status", "OK"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
