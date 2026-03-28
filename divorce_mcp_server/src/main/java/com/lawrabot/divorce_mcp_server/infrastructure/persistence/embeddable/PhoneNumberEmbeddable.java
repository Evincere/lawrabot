package com.lawrabot.divorce_mcp_server.infrastructure.persistence.embeddable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Representación persistible de PhoneNumberVO.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PhoneNumberEmbeddable {

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;
}
