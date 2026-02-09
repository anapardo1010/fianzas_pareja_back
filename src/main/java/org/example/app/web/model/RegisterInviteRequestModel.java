package org.example.app.web.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterInviteRequestModel {
    private String email;
    private String password;
    private String name;
    private Long tenantId; // Para invitar a alguien a un tenant existente
}

