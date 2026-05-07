package org.example.qasystem.service;

import org.example.qasystem.domain.User;
import org.example.qasystem.model.AuthResponse;
import org.example.qasystem.model.LoginRequest;
import org.example.qasystem.model.RegisterRequest;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    User getCurrentUser(String username);
    User updateCurrentUser(String username, String nickname, String email);
}
