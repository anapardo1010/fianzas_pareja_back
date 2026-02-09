package org.example.app.web.model;

import lombok.Getter;
import lombok.Setter;
import lombok.RequiredArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Modelo para crear un nuevo User dentro de un tenant.
 */
@Getter
@Setter
@RequiredArgsConstructor
@NoArgsConstructor(force = true)
public class UserCreateModel implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Long tenantId;
    private final String name;
    private final String email;
    private final BigDecimal contributionPercentage;

    @Override
    public String toString() {
        return "UserCreateModel{" +
                "tenantId=" + tenantId +
                ", name='" + (name != null ? name.replaceAll(".", "*") : null) + '\'' +
                ", email='" + (email != null ? email.replaceAll(".", "*") : null) + '\'' +
                ", contributionPercentage=" + contributionPercentage +
                '}';
    }
}
