package com.example.bankcards.controller;

import com.example.bankcards.dto.user.*;
import com.example.bankcards.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Users (admin)")
@RestController
@RequestMapping("/users")
@Validated
public class UserController {

    private final UserService users;

    public UserController(UserService users) {
        this.users = users;
    }

    @Operation(summary = "Create user (admin)")
    @PostMapping
    public UserResponse create(@RequestBody @Valid CreateUserRequest req) {
        return users.create(req);
    }

    @Operation(summary = "Get user by id (admin)")
    @GetMapping("/{id}")
    public UserResponse get(@PathVariable Long id) {
        return users.get(id);
    }

    @Operation(summary = "List users with pagination and optional email filter (admin)")
    @GetMapping
    public Page<UserResponse> list(@ParameterObject Pageable pageable,
                                   @RequestParam(required = false) String emailLike) {
        return users.list(pageable, emailLike);
    }

    @Operation(summary = "Update basic fields (admin)")
    @PatchMapping("/{id}")
    public UserResponse update(@PathVariable Long id, @RequestBody @Valid UpdateUserRequest req) {
        return users.update(id, req);
    }

    @Operation(summary = "Delete user (admin)")
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        users.delete(id);
    }

    @Operation(summary = "Change own password (admin operating for now)")
    @PostMapping("/{id}/password")
    public void changePassword(@PathVariable Long id, @RequestBody @Valid ChangePasswordRequest req) {
        users.changePassword(id, req);
    }

    @Operation(summary = "Admin sets password directly (admin)")
    @PostMapping("/{id}/password/admin")
    public void adminSetPassword(@PathVariable Long id, @RequestBody @Valid String newPassword) {
        users.adminSetPassword(id, newPassword);
    }

    @Operation(summary = "Replace roles (admin)")
    @PutMapping("/{id}/roles")
    public UserResponse setRoles(@PathVariable Long id, @RequestBody @Valid SetRolesRequest req) {
        return users.setRoles(id, req);
    }

    @Operation(summary = "Add role (admin)")
    @PostMapping("/{id}/roles/{role}")
    public UserResponse addRole(@PathVariable Long id, @PathVariable String role) {
        return users.addRole(id, role);
    }

    @Operation(summary = "Remove role (admin)")
    @DeleteMapping("/{id}/roles/{role}")
    public UserResponse removeRole(@PathVariable Long id, @PathVariable String role) {
        return users.removeRole(id, role);
    }
}