package org.example.qasystem.config;

import org.example.qasystem.domain.Role;
import org.example.qasystem.domain.User;
import org.example.qasystem.repository.RoleRepository;
import org.example.qasystem.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

@Configuration
public class UserSeedInitializer {

    @Bean
    public CommandLineRunner initUsers(UserRepository userRepository,
                                       RoleRepository roleRepository,
                                       PasswordEncoder passwordEncoder) {
        return args -> {
            Role adminRole = roleRepository.findByName("ADMIN").orElseGet(() -> {
                Role role = new Role();
                role.setName("ADMIN");
                return roleRepository.save(role);
            });
            Role userRole = roleRepository.findByName("USER").orElseGet(() -> {
                Role role = new Role();
                role.setName("USER");
                return roleRepository.save(role);
            });

            if (!userRepository.existsByUsername("admin")) {
                User admin = new User();
                admin.setUsername("admin");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setNickname("系统管理员");
                admin.setEmail("admin@qasystem.local");
                admin.setRoles(Set.of(adminRole, userRole));
                userRepository.save(admin);
            }

            if (!userRepository.existsByUsername("user")) {
                User user = new User();
                user.setUsername("user");
                user.setPassword(passwordEncoder.encode("user123"));
                user.setNickname("普通用户");
                user.setEmail("user@qasystem.local");
                user.setRoles(Set.of(userRole));
                userRepository.save(user);
            }
        };
    }
}
