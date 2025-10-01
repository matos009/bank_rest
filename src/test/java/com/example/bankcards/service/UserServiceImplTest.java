package com.example.bankcards.service;

import com.example.bankcards.dto.user.*;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.*;
import com.example.bankcards.repository.RoleRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.service.user.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock UserRepository users;
    @Mock RoleRepository roles;
    @Mock PasswordEncoder encoder;

    UserServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserServiceImpl(users, roles, encoder);
    }

    // ---------- helpers ----------
    private static User user(Long id, String email, String fullName, boolean enabled, Set<Role> roles) {
        User u = new User();
        u.setId(id);
        u.setEmail(email);
        u.setFullName(fullName);
        u.setEnabled(enabled);
        u.setPasswordHash("hashed");
        u.setRoles(roles != null ? roles : new HashSet<>());
        u.setCreatedAt(OffsetDateTime.parse("2024-01-01T00:00:00Z"));
        u.setUpdatedAt(OffsetDateTime.parse("2024-01-02T00:00:00Z"));
        return u;
    }

    private static Role role(short id, String name) {
        Role r = new Role();
        r.setId(id);
        r.setName(name);
        return r;
    }

    // ---------- create ----------
    @Test
    void create_assignsDefaultUSER_whenRolesNotProvided() {
        CreateUserRequest req = new CreateUserRequest("TeSt@Example.COM", "pwd", "John Doe", null);

        when(users.existsByEmail("test@example.com")).thenReturn(false);
        when(encoder.encode("pwd")).thenReturn("ENC");
        when(roles.findByName("USER")).thenReturn(Optional.of(role((short)1, "USER")));

        ArgumentCaptor<User> savedCaptor = ArgumentCaptor.forClass(User.class);
        when(users.save(savedCaptor.capture())).thenAnswer(inv -> {
            User toSave = savedCaptor.getValue();
            toSave.setId(10L);
            return toSave;
        });

        UserResponse resp = service.create(req);

        assertThat(resp.id()).isEqualTo(10L);
        assertThat(resp.email()).isEqualTo("test@example.com");
        assertThat(resp.fullName()).isEqualTo("John Doe");
        assertThat(resp.enabled()).isTrue();
        assertThat(resp.roles()).containsExactly("USER");

        User saved = savedCaptor.getValue();
        assertThat(saved.getPasswordHash()).isEqualTo("ENC");
        verify(users).existsByEmail("test@example.com");
        verify(users).save(any(User.class));
    }

    @Test
    void create_throwsConflict_whenEmailTaken() {
        when(users.existsByEmail("a@b.c")).thenReturn(true);

        assertThatThrownBy(() ->
                service.create(new CreateUserRequest("a@b.c", "x", "n", Set.of("USER"))))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void create_withExplicitRoles_resolvesAllOrFail() {
        CreateUserRequest req = new CreateUserRequest("u@e.com", "pwd", "N", Set.of("user","ADMIN"));

        when(users.existsByEmail("u@e.com")).thenReturn(false);
        when(encoder.encode("pwd")).thenReturn("E");
        when(roles.findByName("USER")).thenReturn(Optional.of(role((short)1, "USER")));
        when(roles.findByName("ADMIN")).thenReturn(Optional.of(role((short)2, "ADMIN")));

        when(users.save(any())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        UserResponse r = service.create(req);
        assertThat(r.roles()).containsExactlyInAnyOrder("USER","ADMIN");
    }

    @Test
    void create_fails_whenUnknownRole() {
        CreateUserRequest req = new CreateUserRequest("x@y.z", "pwd", "N", Set.of("GOD"));
        when(users.existsByEmail("x@y.z")).thenReturn(false);
        when(roles.findByName("GOD")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Role not found: GOD");
    }

    // ---------- get ----------
    @Test
    void get_returnsDtoIncludingRoles() {
        Role r = role((short)1, "USER");
        User u = user(5L, "e@e.e", "F", true, Set.of(r));

        when(users.findWithRolesById(5L)).thenReturn(Optional.of(u));

        UserResponse resp = service.get(5L);
        assertThat(resp.id()).isEqualTo(5L);
        assertThat(resp.roles()).containsExactly("USER");
    }

    @Test
    void get_notFound() {
        when(users.findWithRolesById(7L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(7L))
                .isInstanceOf(NotFoundException.class);
    }

    // ---------- list (pagination + filter) ----------
    @Test
    void list_withoutFilter_pagesAndLoadsRoles() {
        User u1 = user(1L, "a@a", "A", true, new HashSet<>());
        User u2 = user(2L, "b@b", "B", true, new HashSet<>());
        Page<User> page = new PageImpl<>(List.of(u1, u2), PageRequest.of(0, 2), 10);

        when(users.findAll(any(Pageable.class))).thenReturn(page);

        // «Дожимаем» роли через findWithRolesById
        when(users.findWithRolesById(1L)).thenReturn(Optional.of(user(1L, "a@a", "A", true, Set.of(role((short)1,"USER")))));
        when(users.findWithRolesById(2L)).thenReturn(Optional.of(user(2L, "b@b", "B", true, Set.of(role((short)2,"ADMIN")))));

        Page<UserResponse> out = service.list(PageRequest.of(0,2), null);

        assertThat(out.getTotalElements()).isEqualTo(10);
        assertThat(out.getContent()).extracting(UserResponse::id).containsExactly(1L,2L);
        assertThat(out.getContent().get(0).roles()).containsExactly("USER");
        assertThat(out.getContent().get(1).roles()).containsExactly("ADMIN");
    }

    @Test
    void list_withEmailFilter_usesExampleMatcher() {
        // Поведение ExampleMatcher мы не проверяем — это часть Spring Data;
        // мы просто имитируем ответ репозитория при фильтре.
        User u = user(3L, "john@gmail.com", "John", true, new HashSet<>());
        Page<User> page = new PageImpl<>(List.of(u), PageRequest.of(0, 10), 1);

        when(users.findAll(any(Example.class), any(Pageable.class))).thenReturn(page);
        when(users.findWithRolesById(3L)).thenReturn(Optional.of(user(3L, "john@gmail.com", "John", true, Set.of(role((short)1,"USER")))));

        Page<UserResponse> out = service.list(PageRequest.of(0,10), "gmail");
        assertThat(out.getTotalElements()).isEqualTo(1);
        assertThat(out.getContent().get(0).email()).isEqualTo("john@gmail.com");

        // Дополнительно убеждаемся, что метод с Example действительно вызвали
        verify(users).findAll(any(Example.class), any(Pageable.class));
        verify(users, never()).findAll(any(Pageable.class));
    }

    // ---------- update ----------
    @Test
    void update_changesAllowedFields_only() {
        Role r = role((short)1, "USER");
        User existing = user(9L, "x@x", "Old", true, Set.of(r));

        when(users.findWithRolesById(9L)).thenReturn(Optional.of(existing));

        UpdateUserRequest req = new UpdateUserRequest("New Name", false);
        UserResponse out = service.update(9L, req);

        assertThat(out.fullName()).isEqualTo("New Name");
        assertThat(out.enabled()).isFalse();
        // пароль не трогали
        assertThat(existing.getPasswordHash()).isEqualTo("hashed");
        verify(users, never()).save(any()); // JPA dirty checking — метод помечен @Transactional
    }

    @Test
    void update_notFound() {
        when(users.findWithRolesById(100L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.update(100L, new UpdateUserRequest(null, null)))
                .isInstanceOf(NotFoundException.class);
    }

    // ---------- delete ----------
    @Test
    void delete_ok() {
        when(users.existsById(5L)).thenReturn(true);
        service.delete(5L);
        verify(users).deleteById(5L);
    }

    @Test
    void delete_notFound() {
        when(users.existsById(6L)).thenReturn(false);
        assertThatThrownBy(() -> service.delete(6L))
                .isInstanceOf(NotFoundException.class);
        verify(users, never()).deleteById(anyLong());
    }

    // ---------- passwords ----------
    @Test
    void changePassword_ok_whenOldMatches() {
        User u = user(1L, "e", "n", true, Set.of());
        u.setPasswordHash("OLDHASH");

        when(users.findById(1L)).thenReturn(Optional.of(u));
        when(encoder.matches("old", "OLDHASH")).thenReturn(true);
        when(encoder.encode("new")).thenReturn("NEWHASH");

        service.changePassword(1L, new ChangePasswordRequest("old","new"));

        assertThat(u.getPasswordHash()).isEqualTo("NEWHASH");
    }

    @Test
    void changePassword_fails_whenOldDoesNotMatch() {
        User u = user(1L, "e", "n", true, Set.of());
        u.setPasswordHash("H");

        when(users.findById(1L)).thenReturn(Optional.of(u));
        when(encoder.matches("bad", "H")).thenReturn(false);

        assertThatThrownBy(() -> service.changePassword(1L, new ChangePasswordRequest("bad", "new")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Old password is incorrect");
    }

    @Test
    void adminSetPassword_alwaysSets() {
        User u = user(2L, "e", "n", true, Set.of());
        when(users.findById(2L)).thenReturn(Optional.of(u));
        when(encoder.encode("X")).thenReturn("HASHX");

        service.adminSetPassword(2L, "X");
        assertThat(u.getPasswordHash()).isEqualTo("HASHX");
    }

    // ---------- roles management ----------
    @Test
    void setRoles_overwritesAll() {
        Role userRole = role((short)1, "USER");
        Role adminRole = role((short)2, "ADMIN");
        User u = user(1L, "a", "b", true, Set.of(userRole));

        when(users.findWithRolesById(1L)).thenReturn(Optional.of(u));
        when(roles.findByName("USER")).thenReturn(Optional.of(userRole));
        when(roles.findByName("ADMIN")).thenReturn(Optional.of(adminRole));

        SetRolesRequest req = new SetRolesRequest(Set.of("ADMIN","USER"));
        UserResponse out = service.setRoles(1L, req);

        assertThat(out.roles()).containsExactlyInAnyOrder("ADMIN", "USER");
    }

    @Test
    void addRole_adds_whenExists() {
        Role userRole = role((short)1, "USER");
        Role adminRole = role((short)2, "ADMIN");
        User u = user(1L, "a", "b", true, new HashSet<>(Set.of(userRole)));

        when(users.findWithRolesById(1L)).thenReturn(Optional.of(u));
        when(roles.findByName("ADMIN")).thenReturn(Optional.of(adminRole));

        UserResponse out = service.addRole(1L, "admin");

        assertThat(out.roles()).containsExactlyInAnyOrder("USER","ADMIN");
    }

    @Test
    void removeRole_removesIfPresent() {
        Role userRole = role((short)1, "USER");
        Role adminRole = role((short)2, "ADMIN");
        User u = user(1L, "a", "b", true, new HashSet<>(Set.of(userRole, adminRole)));

        when(users.findWithRolesById(1L)).thenReturn(Optional.of(u));

        UserResponse out = service.removeRole(1L, "user");
        assertThat(out.roles()).containsExactly("ADMIN");
    }

    // ---------- getByEmail ----------
    @Test
    void getByEmail_ok() {
        Role r = role((short)1, "USER");
        when(users.findWithRolesByEmail("a@b")).thenReturn(Optional.of(user(5L, "a@b", "n", true, Set.of(r))));

        UserResponse out = service.getByEmail(" a@b ");
        assertThat(out.id()).isEqualTo(5L);
        assertThat(out.roles()).containsExactly("USER");
    }

    @Test
    void getByEmail_notFound() {
        when(users.findWithRolesByEmail("x@y")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getByEmail("x@y"))
                .isInstanceOf(NotFoundException.class);
    }
}