package com.maahfuzdev.moneymanager.dashboard;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard/data-health")
public class DataHealthController {
    private final DataHealthService service;

    public DataHealthController(DataHealthService service) { this.service = service; }

    @GetMapping
    public DataHealthResponse check(@AuthenticationPrincipal Jwt jwt) {
        return service.check(jwt.getSubject());
    }
}
