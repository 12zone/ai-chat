package org.example.qasystem.service.impl;

import org.example.qasystem.domain.Role;
import org.example.qasystem.domain.User;
import org.example.qasystem.model.AuthResponse;
import org.example.qasystem.model.LoginRequest;
import org.example.qasystem.model.RegisterRequest;
import org.example.qasystem.repository.RoleRepository;
import org.example.qasystem.repository.UserRepository;
import org.example.qasystem.security.JwtTokenProvider;
import org.example.qasystem.service.AuthService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    public AuthServiceImpl(UserRepository userRepository,
                           RoleRepository roleRepository,
                           PasswordEncoder passwordEncoder,
                           JwtTokenProvider jwtTokenProvider,
                           AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.authenticationManager = authenticationManager;
    }

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("用户名已存在");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("邮箱已存在");
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname() == null || request.getNickname().isBlank() ? request.getUsername() : request.getNickname());
        user.setEmail(request.getEmail());
        user.setRoles(resolveRoles(request.getRoles()));
        User saved = userRepository.save(user);
        return buildAuthResponse(saved);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("用户名或密码错误"));
        return buildAuthResponse(user);
    }

    @Override
    public User getCurrentUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
    }

    @Override
    public User updateCurrentUser(String username, String nickname, String email) {
        User user = getCurrentUser(username);
        if (nickname != null && !nickname.isBlank()) {
            user.setNickname(nickname);
        }
        if (email != null && !email.isBlank() && !email.equalsIgnoreCase(user.getEmail())) {
            if (userRepository.existsByEmail(email)) {
                throw new IllegalArgumentException("邮箱已存在");
            }
            user.setEmail(email);
        }
        return userRepository.save(user);
    }

    private Set<Role> resolveRoles(List<String> roles) {
        List<String> safeRoles = (roles == null || roles.isEmpty()) ? List.of("USER") : roles;
        Set<Role> roleSet = new HashSet<>();
        for (String roleName : safeRoles) {
            String normalized = roleName == null ? "USER" : roleName.trim().toUpperCase();
            if (normalized.isEmpty()) {
                normalized = "USER";
            }
            final String roleKey = normalized;
            Role role = roleRepository.findByName(roleKey).orElseGet(() -> {
                Role newRole = new Role();
                newRole.setName(roleKey);
                return roleRepository.save(newRole);
            });
            roleSet.add(role);
        }
        return roleSet;
    }

    private AuthResponse buildAuthResponse(User user) {
        List<String> roles = user.getRoles().stream().map(Role::getName).sorted().collect(Collectors.toList());
        String token = jwtTokenProvider.createToken(user.getUsername(), roles);
        return new AuthResponse(token, user.getUsername(), user.getNickname(), roles);
    }
}
