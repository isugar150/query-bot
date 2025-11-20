package com.namejm.query_bot.service;

import com.namejm.query_bot.domain.AdminUser;
import com.namejm.query_bot.dto.AuthResponse;
import com.namejm.query_bot.repository.AdminUserRepository;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthService(AdminUserRepository adminUserRepository, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    public boolean hasAdmin() {
        return adminUserRepository.count() > 0;
    }

    public AdminUser createAdmin(String username, String password) {
        if (adminUserRepository.existsByUsername(username)) {
            throw new IllegalStateException("이미 해당 아이디가 존재합니다.");
        }
        AdminUser admin = new AdminUser();
        admin.setUsername(username);
        admin.setPasswordHash(passwordEncoder.encode(password));
        return adminUserRepository.save(admin);
    }

    public Optional<AuthResponse> login(String username, String password) {
        return adminUserRepository.findByUsername(username)
                .filter(user -> passwordEncoder.matches(password, user.getPasswordHash()))
                .map(user -> new AuthResponse(
                        user.getUsername(),
                        tokenService.generateAccessToken(user.getUsername()),
                        tokenService.generateRefreshToken(user.getUsername())
                ));
    }

    public Optional<AuthResponse> refresh(String refreshToken) {
        if (!tokenService.isRefreshToken(refreshToken)) {
            return Optional.empty();
        }
        return tokenService.parseUsername(refreshToken)
                .flatMap(adminUserRepository::findByUsername)
                .map(user -> new AuthResponse(
                        user.getUsername(),
                        tokenService.generateAccessToken(user.getUsername()),
                        tokenService.generateRefreshToken(user.getUsername())
                ));
    }
}
