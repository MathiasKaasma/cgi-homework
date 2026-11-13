package com.example.homework.blacklist;

import jakarta.validation.constraints.NotBlank;

public record BlacklistRequest(@NotBlank String recipient, String reason) {
}
