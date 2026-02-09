package org.example.app.facade;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;
import java.util.List;
import org.example.app.domain.entity.User;
import org.example.app.domain.repository.UserRepository;

@Component
@Transactional(readOnly = true)
public class UserFacade {

    private final UserRepository userRepository;

    public UserFacade(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Page<User> findPage(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    public Page<User> findAll(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Transactional(readOnly = false)
    public User save(User entity) {
        return userRepository.save(entity);
    }

    public List<User> findByTenantIdAndActive(Long tenantId) {
        return userRepository.findByTenant_IdAndIsActiveTrue(tenantId);
    }

    public Page<User> findByTenantIdAndActive(Long tenantId, Pageable pageable) {
        return userRepository.findByTenant_IdAndIsActiveTrue(tenantId, pageable);
    }

    public Optional<User> findByEmailAndTenantAndActive(String email, Long tenantId) {
        return userRepository.findByEmailAndTenant_IdAndIsActiveTrue(email, tenantId);
    }

    public boolean existsByEmail(String email) {
        return userRepository.findByEmail(email).isPresent();
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Transactional(readOnly = false)
    public void delete(User entity) {
        userRepository.delete(entity);
    }
}
