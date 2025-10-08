package com.example.bankcards.service.auth;


import com.example.bankcards.dto.login.LoginRequest;
import com.example.bankcards.dto.login.LoginResponse;

public interface AuthService {
   LoginResponse login(LoginRequest req);
}