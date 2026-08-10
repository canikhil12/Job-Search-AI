package com.jobmatch.auth.dto;

import com.jobmatch.user.User;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String fullName
) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getFullName());
    }
}
