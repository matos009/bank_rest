package com.example.bankcards.service.user;

import com.example.bankcards.dto.user.*;
import com.example.bankcards.entity.User;
import org.springframework.data.domain.*;

public interface UserService {
    UserResponse create(CreateUserRequest req);                      // регистрация (админ/seed)
    UserResponse get(Long id);                                       // получить 1
    Page<UserResponse> list(Pageable pageable, String emailLike);    // поиск + пагинация
    UserResponse update(Long id, UpdateUserRequest req);             // обновить ФИО/enabled
    void delete(Long id);                                            // удалить

    void changePassword(Long id, ChangePasswordRequest req);         // со старым паролем
    void adminSetPassword(Long id, String newPassword);              // без старого (админ)

    UserResponse setRoles(Long id, SetRolesRequest req);             // заменить роли
    UserResponse addRole(Long id, String role);                      // добавить роль
    UserResponse removeRole(Long id, String role);                   // убрать роль

    User getEntityByEmailOrThrow(String email);
    UserResponse getByEmail(String email);
}