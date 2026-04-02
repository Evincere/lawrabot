package com.lawrabot.divorce_mcp_server.infrastructure.mcp.dto;

import java.util.UUID;

public record CitizenDto(
    UUID id,
    String dni,
    String cuil,
    String fullName,
    String phoneNumber,
    String email,
    String address
) {}
