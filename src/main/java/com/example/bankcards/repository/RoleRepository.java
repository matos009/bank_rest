package com.example.bankcards.repository;

import com.example.bankcards.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Short> {
    Optional<Role> findByName(String name);
    boolean existsByName(String name);
}
