package org.example.app.facade;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;
import java.util.List;
import org.example.app.domain.entity.Category;
import org.example.app.domain.repository.CategoryRepository;

@Component
@Transactional(readOnly = true)
public class CategoryFacade {

    private final CategoryRepository categoryRepository;

    public CategoryFacade(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public Page<Category> findPage(Pageable pageable) {
        return categoryRepository.findAll(pageable);
    }

    public Page<Category> findAll(Pageable pageable) {
        return categoryRepository.findAll(pageable);
    }

    public Optional<Category> findById(Long id) {
        return categoryRepository.findById(id);
    }

    @Transactional(readOnly = false)
    public Category save(Category entity) {
        return categoryRepository.save(entity);
    }

    public List<Category> findByTenantIdAndActive(Long tenantId) {
        return categoryRepository.findByTenant_IdAndIsActiveTrue(tenantId);
    }

    public Page<Category> findByTenantIdAndActive(Long tenantId, Pageable pageable) {
        return categoryRepository.findByTenant_IdAndIsActiveTrue(tenantId, pageable);
    }

    public Optional<Category> findByNameAndTenantAndActive(String name, Long tenantId) {
        return categoryRepository.findByNameAndTenant_IdAndIsActiveTrue(name, tenantId);
    }

    public boolean existsByNameAndTenant(String name, Long tenantId) {
        return categoryRepository.findByNameAndTenant_IdAndIsActiveTrue(name, tenantId).isPresent();
    }

    @Transactional(readOnly = false)
    public void delete(Category entity) {
        categoryRepository.delete(entity);
    }
}
