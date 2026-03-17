package com.example.musicbooru.auth;

import com.example.musicbooru.model.Role;

public record RegisterRequest(
        String username,
        String password,
        Role role
) {
}