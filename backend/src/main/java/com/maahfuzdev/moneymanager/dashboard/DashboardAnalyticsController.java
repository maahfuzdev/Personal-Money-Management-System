package com.maahfuzdev.moneymanager.dashboard;

import jakarta.validation.constraints.Pattern;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardAnalyticsController {

    private final DashboardAnalyticsService analyticsService;

    public DashboardAnalyticsController(DashboardAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/analytics")
    public DashboardAnalyticsResponse analytics(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) @Pattern(regexp = "\\d{4}-(0[1-9]|1[0-2])", message = "Use YYYY-MM format") String month) {
        return analyticsService.analytics(jwt.getSubject(), month);
    }
}
