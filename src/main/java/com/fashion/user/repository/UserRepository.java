package com.fashion.user.repository;

import com.fashion.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByPhoneAndEmailNot(String phone, String email);
    long countByRole_NameIgnoreCase(String roleName);
    java.util.List<User> findAllByRole_NameIgnoreCase(String roleName, Sort sort);
}
