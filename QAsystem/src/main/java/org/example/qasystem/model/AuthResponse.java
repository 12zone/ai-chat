package org.example.qasystem.model;

import java.util.List;

public class AuthResponse {
    private String token;
    private String username;
    private String nickname;
    private List<String> roles;

    public AuthResponse(String token, String username, String nickname, List<String> roles) {
        this.token = token;
        this.username = username;
        this.nickname = nickname;
        this.roles = roles;
    }

    public String getToken() {
        return token;
    }

    public String getUsername() {
        return username;
    }

    public String getNickname() {
        return nickname;
    }

    public List<String> getRoles() {
        return roles;
    }
}
