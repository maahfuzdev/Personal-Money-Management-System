package com.maahfuzdev.moneymanager.backup;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/account-backup")
public class AccountBackupController {
    private final AccountBackupService backupService;
    public AccountBackupController(AccountBackupService backupService) { this.backupService = backupService; }

    @GetMapping(value = "/export.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AccountBackupResponse> export(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok().contentType(new MediaType("application", "json", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("money-manager-backup.json", StandardCharsets.UTF_8).build().toString())
                .body(backupService.export(jwt.getSubject()));
    }
}
