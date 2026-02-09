package org.example.app.web.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseModel {
    private String token;
    private Long userId;
    private Long tenantId;
    private String role;
}

