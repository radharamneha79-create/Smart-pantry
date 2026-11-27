package com.smartpantry.dto;

public record CsrfResponse(String token, String headerName) {
}
