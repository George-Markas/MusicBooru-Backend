package com.example.musicbooru.dto;

import com.example.musicbooru.model.Role;

public record UserInfoResponse(
        String username,
        Role role
) {
}
