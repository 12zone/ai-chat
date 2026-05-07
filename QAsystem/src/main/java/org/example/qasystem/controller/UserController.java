package org.example.qasystem.controller;

import org.example.qasystem.domain.Role;
import org.example.qasystem.domain.User;
import org.example.qasystem.model.UserProfileRequest;
import org.example.qasystem.model.UserProfileResponse;
import org.example.qasystem.service.AuthService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "*")
public class UserController {
    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/me")
    public UserProfileResponse me(Authentication authentication) {
        User user = authService.getCurrentUser(authentication.getName());
        return toResponse(user);
    }

    @PutMapping("/me")
    public UserProfileResponse updateMe(@RequestBody UserProfileRequest request, Authentication authentication) {
        User user = authService.updateCurrentUser(authentication.getName(), request.getNickname(), request.getEmail());
        return toResponse(user);
    }

    private UserProfileResponse toResponse(User user) {
        List<String> roles = user.getRoles().stream().map(Role::getName).sorted().toList();
        return new UserProfileResponse(user.getId(), user.getUsername(), user.getNickname(), user.getEmail(), roles);
    }
}
