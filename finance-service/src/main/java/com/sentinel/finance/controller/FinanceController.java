package com.sentinel.finance.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/finance")
public class FinanceController {

    private static final Logger logger = LoggerFactory.getLogger(FinanceController.class);

    @GetMapping("/me")
    @PreAuthorize("hasRole('employees')")
    public Map<String, Object> getMe(Authentication authentication) {
        logger.info("Accessing /me endpoint");
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String username = jwt.getClaimAsString("preferred_username");
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return Map.of(
                "username", username != null ? username : jwt.getSubject(),
                "roles", roles
        );
    }

    @GetMapping("/admin/salaries")
    @PreAuthorize("hasRole('finance-admins')")
    public List<Map<String, Object>> getSalaries() {
        logger.info("Accessing /admin/salaries endpoint");
        return List.of(
                Map.of("id", 1, "employee", "shree.admin", "salary", 150000),
                Map.of("id", 2, "employee", "rishabh.user", "salary", 90000),
                Map.of("id", 3, "employee", "aman.user", "salary", 85000)
        );
    }
}
