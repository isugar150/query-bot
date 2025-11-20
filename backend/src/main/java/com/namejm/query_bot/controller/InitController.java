package com.namejm.query_bot.controller;

import com.namejm.query_bot.domain.DatabaseConnection;
import com.namejm.query_bot.dto.AuthResponse;
import com.namejm.query_bot.dto.DbSummary;
import com.namejm.query_bot.dto.InitRequest;
import com.namejm.query_bot.dto.InitStatusResponse;
import com.namejm.query_bot.service.AuthService;
import com.namejm.query_bot.service.DatabaseService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/init")
public class InitController {

    private final AuthService authService;
    private final DatabaseService databaseService;

    public InitController(AuthService authService, DatabaseService databaseService) {
        this.authService = authService;
        this.databaseService = databaseService;
    }

    @GetMapping("/status")
    public InitStatusResponse status() {
        return new InitStatusResponse(authService.hasAdmin());
    }

    @PostMapping("/setup")
    public ResponseEntity<?> setup(@Valid @RequestBody InitRequest request) throws Exception {
        if (authService.hasAdmin()) {
            return ResponseEntity.badRequest().body("이미 초기 설정이 완료되었습니다.");
        }
        authService.createAdmin(request.admin().username(), request.admin().password());
        DatabaseConnection savedDb = null;
        if (request.database() != null) {
            var schema = databaseService.reloadSchema(request.database());
            savedDb = databaseService.register(request.database(), schema);
        }
        AuthResponse auth = authService.login(request.admin().username(), request.admin().password())
                .orElseThrow(() -> new IllegalStateException("로그인 토큰 발급 실패"));

        DbSummary summary = savedDb == null ? null : new DbSummary(
                savedDb.getId(),
                savedDb.getName(),
                savedDb.getDbType(),
                savedDb.getHost(),
                savedDb.getPort(),
                savedDb.getDatabaseName()
        );
        return ResponseEntity.ok(new InitSetupResponse(auth, summary));
    }

    public record InitSetupResponse(
            AuthResponse auth,
            DbSummary database
    ) {
    }
}
