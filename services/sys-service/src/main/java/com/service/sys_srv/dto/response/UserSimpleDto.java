package com.service.sys_srv.dto.response;

import java.util.UUID;

public record UserSimpleDto(UUID id, String fullName, String avatarUrl) {}