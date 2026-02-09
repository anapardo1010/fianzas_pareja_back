package org.example.app.facade;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;
import java.util.List;
import org.example.app.domain.entity.Tenant;
import org.example.app.domain.repository.TenantRepository;

@Component
@Transactional(readOnly = true)
public class TenantFacade {

    private final TenantRepository tenantRepository;

    public TenantFacade(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    public Page<Tenant> findPage(Pageable pageable) {
        return tenantRepository.findAll(pageable);
    }

    public Page<Tenant> findAll(Pageable pageable) {
        return tenantRepository.findAll(pageable);
    }

    public Page<Tenant> findByIsActive(boolean isActive, Pageable pageable) {
        return tenantRepository.findByIsActive(isActive, pageable);
    }

    public Optional<Tenant> findById(Long id) {
        return tenantRepository.findById(id);
    }

    @Transactional(readOnly = false)
    public Tenant save(Tenant entity) {
        return tenantRepository.save(entity);
    }

    public boolean existsByGroupName(String groupName) {
        return tenantRepository.findByGroupNameAndIsActiveTrue(groupName).isPresent();
    }

    public Optional<Tenant> findByGroupNameAndActive(String groupName) {
        return tenantRepository.findByGroupNameAndIsActiveTrue(groupName);
    }

    public List<Tenant> findByPlanTypeAndActive(String planType) {
        return tenantRepository.findByPlanTypeAndIsActiveTrue(planType);
    }

    public long countActiveTenanats() {
        return tenantRepository.countByIsActiveTrue();
    }

    @Transactional(readOnly = false)
    public void delete(Tenant entity) {
        tenantRepository.delete(entity);
    }
}
