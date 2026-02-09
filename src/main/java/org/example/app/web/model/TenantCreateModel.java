package org.example.app.web.model;

import lombok.Getter;
import lombok.Setter;
import lombok.RequiredArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Modelo para crear un nuevo Tenant (registro de pareja/grupo).
 * Utilizado en el proceso de onboarding.
 */
@Getter
@Setter
@RequiredArgsConstructor
@NoArgsConstructor(force = true)
public class TenantCreateModel implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String groupName;
    private final String planType;
    private final Boolean isActive;

    @Override
    public String toString() {
        return "TenantCreateModel{" +
                "groupName='" + (groupName != null ? groupName.replaceAll(".", "*") : null) + '\'' +
                ", planType='" + planType + '\'' +
                '}';
    }
}
