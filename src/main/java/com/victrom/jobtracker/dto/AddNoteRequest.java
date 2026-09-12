package com.victrom.jobtracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddNoteRequest(@NotBlank @Size(max = 3000) String message) {
}
