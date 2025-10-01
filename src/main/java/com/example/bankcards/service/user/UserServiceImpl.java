package com.example.bankcards.service.user;

import com.example.bankcards.dto.user.*;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.*;
import com.example.bankcards.repository.RoleRepository;
import com.example.bankcards.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder encoder;

    public UserServiceImpl(UserRepository users, RoleRepository roles, PasswordEncoder encoder) {
        this.users = users;
        this.roles = roles;
        this.encoder = encoder;
    }

    @Override
    public UserResponse create(CreateUserRequest req) {
        String email = req.email().trim().toLowerCase();
        if (users.existsByEmail(email)) {
            throw new ConflictException("Email already taken: " + email);
        }

        User u = new User();
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(req.password()));
        u.setFullName(req.fullName());
        u.setEnabled(true);

        // Роли: если не передали — назначим USER
        Set<String> roleNames = (req.roles() == null || req.roles().isEmpty())
                ? Set.of("USER")
                : req.roles().stream().map(r -> r.trim().toUpperCase()).collect(Collectors.toSet());

        u.setRoles(resolveRoles(roleNames));

        User saved = users.save(u);
        return toDto(saved);
    }

    @Override
    public UserResponse get(Long id) {
        User u = users.findWithRolesById(id).orElseThrow(() -> new NotFoundException("User not found: " + id));
        return toDto(u);
    }

    @Override
    public Page<UserResponse> list(Pageable pageable, String emailLike) {
        Page<User> page;
        if (emailLike == null || emailLike.isBlank()) {
            page = users.findAll(pageable);
        } else {
            User probe = new User();
            probe.setEmail(emailLike);
            ExampleMatcher matcher = ExampleMatcher.matching()
                    .withMatcher("email", ExampleMatcher.GenericPropertyMatchers.contains().ignoreCase())
                    .withIgnorePaths("enabled", "passwordHash", "fullName", "createdAt", "updatedAt", "id");
            page = users.findAll(Example.of(probe, matcher), pageable);
        }
        // Жадная загрузка ролей для страницы
        List<UserResponse> mapped = page.getContent().stream()
                .map(u -> users.findWithRolesById(u.getId()).orElse(u)) // подгрузим роли, если не подгружены, небольшой костыль конечно, но времени мало и это как бы тестовое
                .map(this::toDto)
                .toList();

        return new PageImpl<>(mapped, pageable, page.getTotalElements());
    }

    @Override
    public UserResponse update(Long id, UpdateUserRequest req) {
        User u = users.findWithRolesById(id).orElseThrow(() -> new NotFoundException("User not found: " + id));

        if (req.fullName() != null) u.setFullName(req.fullName());
        if (req.enabled() != null)  u.setEnabled(req.enabled());

        return toDto(u);
    }

    @Override
    public void delete(Long id) {
        if (!users.existsById(id)) throw new NotFoundException("User not found: " + id);
        users.deleteById(id);
    }

    @Override
    public void changePassword(Long id, ChangePasswordRequest req) {
        User u = users.findById(id).orElseThrow(() -> new NotFoundException("User not found: " + id));
        if (!encoder.matches(req.oldPassword(), u.getPasswordHash())) {
            throw new BusinessException("Old password is incorrect");
        }
        u.setPasswordHash(encoder.encode(req.newPassword()));
    }

    @Override
    public void adminSetPassword(Long id, String newPassword) {
        User u = users.findById(id).orElseThrow(() -> new NotFoundException("User not found: " + id));
        u.setPasswordHash(encoder.encode(newPassword));
    }

    @Override
    public UserResponse setRoles(Long id, SetRolesRequest req) {
        User u = users.findWithRolesById(id).orElseThrow(() -> new NotFoundException("User not found: " + id));
        Set<String> names = req.roles().stream().map(s -> s.trim().toUpperCase()).collect(Collectors.toSet());
        u.setRoles(resolveRoles(names));
        return toDto(u);
    }

    @Override
    public UserResponse addRole(Long id, String role) {
        User u = users.findWithRolesById(id).orElseThrow(() -> new NotFoundException("User not found: " + id));
        Role r = roles.findByName(role.trim().toUpperCase()).orElseThrow(() -> new NotFoundException("Role not found: " + role));
        u.getRoles().add(r);
        return toDto(u);
    }

    @Override
    public UserResponse removeRole(Long id, String role) {
        User u = users.findWithRolesById(id).orElseThrow(() -> new NotFoundException("User not found: " + id));
        String target = role.trim().toUpperCase();
        u.getRoles().removeIf(r -> r.getName().equalsIgnoreCase(target));
        return toDto(u);
    }
    @Override
    public User getEntityByEmailOrThrow(String email) {
        return users.findWithRolesByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new NotFoundException("User not found by email: " + email));
    }

    @Override
    public UserResponse getByEmail(String email) {
        User u = users.findWithRolesByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new NotFoundException("User not found by email: " + email));
        return toDto(u);
    }

    // ===== helpers =====

    private Set<Role> resolveRoles(Set<String> roleNames) {
        Set<Role> rs = new HashSet<>();
        for (String name : roleNames) {
            Role r = roles.findByName(name).orElseThrow(() -> new NotFoundException("Role not found: " + name));
            rs.add(r);
        }
        return rs;
    }

    private UserResponse toDto(User u) {
        return new UserResponse(
                u.getId(),
                u.getEmail(),
                u.getFullName(),
                u.isEnabled(),
                u.getRoles().stream().map(Role::getName).collect(Collectors.toSet()),
                u.getCreatedAt(),
                u.getUpdatedAt()
        );
    }
}