package org.example.qasystem.model;

import java.util.List;

public class UserProfileResponse {
    private Long id;
    private String username;
    private String nickname;
    private String email;
    private List<String> roles;

    public UserProfileResponse(Long id, String username, String nickname, String email, List<String> roles) {
        this.id = id;
        this.username = username;
        this.nickname = nickname;
        this.email = email;
        this.roles = roles;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getNickname() {
        return nickname;
    }

    public String getEmail() {
        return email;
    }

    public List<String> getRoles() {
        return roles;
    }
}
