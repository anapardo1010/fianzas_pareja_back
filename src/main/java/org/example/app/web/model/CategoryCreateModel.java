package org.example.app.web.model;

import lombok.Getter;
import lombok.Setter;
import lombok.RequiredArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Modelo para crear una nueva Category dentro de un tenant.
 */
@Getter
@Setter
@RequiredArgsConstructor
@NoArgsConstructor(force = true)
public class CategoryCreateModel implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Long tenantId;
    private final String name;
    private final String description;

    @Override
    public String toString() {
        return "CategoryCreateModel{" +
                "tenantId=" + tenantId +
                ", name='" + (name != null ? name.replaceAll(".", "*") : null) + '\'' +
                ", description='" + (description != null ? description.replaceAll(".", "*") : null) + '\'' +
                '}';
    }
}
