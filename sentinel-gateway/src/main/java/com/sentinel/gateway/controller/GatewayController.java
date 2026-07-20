package com.sentinel.gateway.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@RestController
public class GatewayController {

    @GetMapping("/whoami")
    public Mono<Map<String, Object>> whoami(@AuthenticationPrincipal OidcUser oidcUser) {
        Map<String, Object> userInfo = new HashMap<>();
        if (oidcUser != null) {
            userInfo.put("name", oidcUser.getPreferredUsername());
            userInfo.put("groups", oidcUser.getClaim("groups"));
        } else {
            userInfo.put("error", "No authenticated user");
        }
        return Mono.just(userInfo);
    }
}
