package com.service.sys_srv.dto.response;

import java.util.UUID;

public record DoctorSearchResponseDto(
        UUID id,
        String fullName,
        String specialty,
        String degree
) {}