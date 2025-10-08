package com.example.bankcards.service.user;

import com.example.bankcards.dto.user.*;
import com.example.bankcards.entity.User;
import org.springframework.data.domain.*;

public interface UserService {
    UserResponse create(CreateUserRequest req);
    UserResponse get(Long id);
    Page<UserResponse> list(Pageable pageable, String emailLike);
    UserResponse update(Long id, UpdateUserRequest req);
    void delete(Long id);

    void changePassword(Long id, ChangePasswordRequest req);
    void adminSetPassword(Long id, String newPassword);

    UserResponse setRoles(Long id, SetRolesRequest req);
    UserResponse addRole(Long id, String role);
    UserResponse removeRole(Long id, String role);

    User getEntityByEmailOrThrow(String email);
    UserResponse getByEmail(String email);
}