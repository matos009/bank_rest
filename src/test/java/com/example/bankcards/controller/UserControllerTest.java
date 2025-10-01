package com.example.bankcards.controller;

import com.example.bankcards.dto.user.*;
import com.example.bankcards.service.user.UserService;
import com.example.bankcards.web.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.json.BasicJsonTester;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.Set;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;


import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UserController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    @MockBean UserService users;

    // ===== helpers =====
    private UserResponse sampleUser() {
        return new UserResponse(
                42L,
                "john@example.com",
                "John Doe",
                true,
                Set.of("USER","ADMIN"),
                OffsetDateTime.parse("2025-01-01T10:00:00Z"),
                OffsetDateTime.parse("2025-01-02T10:00:00Z")
        );
    }

    // ---------- CREATE ----------
    @Test
    void create_user_ok() throws Exception {
        var req = new CreateUserRequest("john@example.com", "pass123", "John Doe", Set.of("USER"));
        var resp = sampleUser();

        given(users.create(any(CreateUserRequest.class))).willReturn(resp);

        mvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.roles", containsInAnyOrder("USER","ADMIN")));
    }

    // ---------- GET ----------
    @Test
    void get_user_by_id_ok() throws Exception {
        given(users.get(42L)).willReturn(sampleUser());

        mvc.perform(get("/users/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.enabled").value(true));
    }

    // ---------- LIST (page + filter) ----------
    @Test
    void list_users_paged_filtered_ok() throws Exception {
        var u = sampleUser();
        var page = new PageImpl<>(java.util.List.of(u), PageRequest.of(0, 2, Sort.by("id").descending()), 1);

        given(users.list(any(Pageable.class), eq("john"))).willReturn(page);

        mvc.perform(get("/users")
                        .param("page","0")
                        .param("size","2")
                        .param("sort","id,desc")
                        .param("emailLike","john"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(42))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // ---------- UPDATE ----------
    @Test
    void update_user_ok() throws Exception {
        var req = new UpdateUserRequest("Johnny", true);
        var updated = new UserResponse(
                42L,"john@example.com","Johnny",true,Set.of("USER"),
                OffsetDateTime.parse("2025-01-01T10:00:00Z"),
                OffsetDateTime.parse("2025-01-03T10:00:00Z")
        );

        given(users.update(eq(42L), any(UpdateUserRequest.class))).willReturn(updated);

        mvc.perform(patch("/users/42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Johnny"))
                .andExpect(jsonPath("$.enabled").value(true));
    }

    // ---------- DELETE ----------
    @Test
    void delete_user_ok() throws Exception {
        doNothing().when(users).delete(42L);

        mvc.perform(delete("/users/42"))
                .andExpect(status().isOk());

        verify(users).delete(42L);
    }

    // ---------- CHANGE PASSWORD (self) ----------
    @Test
    void change_password_ok() throws Exception {
        var req = new ChangePasswordRequest("old","newStrong!");
        doNothing().when(users).changePassword(eq(42L), any(ChangePasswordRequest.class));

        mvc.perform(post("/users/42/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk());

        verify(users).changePassword(eq(42L), any(ChangePasswordRequest.class));
    }

    // ---------- ADMIN SET PASSWORD ----------
    @Test
    void admin_set_password_ok() throws Exception {
        doNothing().when(users).adminSetPassword(42L, "qwerty");

        // тело — сырой текст; выставляем text/plain
        mvc.perform(post("/users/42/password/admin")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("qwerty"))
                .andExpect(status().isOk());

        verify(users).adminSetPassword(42L, "qwerty");
    }

    // ---------- SET ROLES ----------
    @Test
    void set_roles_ok() throws Exception {
        var req = new SetRolesRequest(Set.of("ADMIN","USER"));
        var resp = new UserResponse(42L, "john@example.com", "John", true, req.roles(),
                OffsetDateTime.parse("2025-01-01T10:00:00Z"),
                OffsetDateTime.parse("2025-01-02T10:00:00Z"));

        given(users.setRoles(eq(42L), any(SetRolesRequest.class))).willReturn(resp);

        mvc.perform(put("/users/42/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles", containsInAnyOrder("USER","ADMIN")));
    }

    // ---------- ADD ROLE ----------
    @Test
    void add_role_ok() throws Exception {
        var resp = new UserResponse(42L, "john@example.com", "John", true, Set.of("USER","ADMIN"),
                OffsetDateTime.parse("2025-01-01T10:00:00Z"),
                OffsetDateTime.parse("2025-01-02T10:00:00Z"));

        given(users.addRole(42L, "ADMIN")).willReturn(resp);

        mvc.perform(post("/users/42/roles/ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles", hasItem("ADMIN")));
    }

    // ---------- REMOVE ROLE ----------
    @Test
    void remove_role_ok() throws Exception {
        var resp = new UserResponse(42L, "john@example.com", "John", true, Set.of("USER"),
                OffsetDateTime.parse("2025-01-01T10:00:00Z"),
                OffsetDateTime.parse("2025-01-02T10:00:00Z"));

        given(users.removeRole(42L, "ADMIN")).willReturn(resp);

        mvc.perform(delete("/users/42/roles/ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles", not(hasItem("ADMIN"))));
    }
}