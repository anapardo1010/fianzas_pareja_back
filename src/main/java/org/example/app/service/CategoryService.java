package org.example.app.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.app.domain.entity.Category;
import org.example.app.domain.entity.Tenant;
import org.example.app.facade.CategoryFacade;
import org.example.app.facade.TenantFacade;
import org.example.app.web.model.CategoryCreateModel;
import org.example.app.web.model.CategoryModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Servicio para la gestión de Categories (maestros de categorías).
 * Implementa la lógica de negocio para categorías personalizadas por tenant.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {

    private final CategoryFacade categoryFacade;
    private final TenantFacade tenantFacade;

    /**
     * Crea una nueva categoría dentro de un tenant.
     */
    @Transactional
    public CategoryModel createCategory(CategoryCreateModel createModel) {
        log.info("Creando categoría: {} para tenant: {}", createModel.getName(), createModel.getTenantId());

        // Validar que el tenant existe
        Tenant tenant = tenantFacade.findById(createModel.getTenantId())
                .orElseThrow(() -> new IllegalArgumentException("Tenant no encontrado: " + createModel.getTenantId()));

        // Validar nombre único dentro del tenant
        validateUniqueCategoryName(createModel.getName(), createModel.getTenantId());

        // Crear categoría
        Category category = new Category(
            tenant,
            createModel.getName(),
            createModel.getDescription(),
            true
        );

        Category savedCategory = categoryFacade.save(category);
        log.info("Categoría creada con ID: {}", savedCategory.getId());

        return CategoryModel.FN_ENTITY_TO_MODEL.apply(savedCategory);
    }

    /**
     * Busca categorías activas por tenant.
     */
    public List<CategoryModel> findByTenantList(Long tenantId) {
        log.debug("Buscando categorías para tenant: {}", tenantId);
        return categoryFacade.findByTenantIdAndActive(tenantId)
                .stream()
                .map(CategoryModel.FN_ENTITY_TO_MODEL)
                .collect(Collectors.toList());
    }

    /**
     * Busca categorías activas por tenant con paginación.
     */
    public Page<CategoryModel> findByTenant(Long tenantId, Pageable pageable) {
        log.debug("Buscando categorías para tenant: {} con paginación", tenantId);
        return categoryFacade.findByTenantIdAndActive(tenantId, pageable)
                .map(CategoryModel.FN_ENTITY_TO_MODEL);
    }

    /**
     * Busca todas las categorías con paginación.
     */
    public Page<CategoryModel> findAll(Pageable pageable) {
        log.debug("Buscando todas las categorías con paginación");
        return categoryFacade.findAll(pageable)
                .map(CategoryModel.FN_ENTITY_TO_MODEL);
    }

    /**
     * Busca una categoría por ID.
     */
    public Optional<CategoryModel> findById(Long id) {
        log.debug("Buscando categoría por ID: {}", id);
        return categoryFacade.findById(id)
                .map(CategoryModel.FN_ENTITY_TO_MODEL);
    }

    /**
     * Actualiza una categoría.
     */
    @Transactional
    public CategoryModel updateCategory(Long categoryId, CategoryCreateModel updateModel) {
        log.info("Actualizando categoría: {}", categoryId);

        Category category = categoryFacade.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada: " + categoryId));

        // Si el nombre cambió, validar unicidad
        if (!category.getName().equals(updateModel.getName())) {
            validateUniqueCategoryName(updateModel.getName(), category.getTenant().getId());
            category.setName(updateModel.getName());
        }

        if (updateModel.getDescription() != null) {
            category.setDescription(updateModel.getDescription());
        }

        category.setUpdatedAt(LocalDateTime.now());

        Category updatedCategory = categoryFacade.save(category);
        return CategoryModel.FN_ENTITY_TO_MODEL.apply(updatedCategory);
    }

    /**
     * Elimina (desactiva) una categoría.
     */
    @Transactional
    public void deleteCategory(Long categoryId) {
        log.info("Eliminando categoría: {}", categoryId);

        Category category = categoryFacade.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada: " + categoryId));

        category.setIsActive(false);
        category.setUpdatedAt(LocalDateTime.now());
        categoryFacade.save(category);
    }

    // Métodos privados de validación

    private void validateUniqueCategoryName(String name, Long tenantId) {
        if (categoryFacade.existsByNameAndTenant(name, tenantId)) {
            throw new IllegalArgumentException("Ya existe una categoría con el nombre: " + name);
        }
    }
}
