package com.sentinel.finance;

import com.sentinel.finance.controller.FinanceController;
import com.sentinel.finance.filter.CorrelationIdFilter;
import com.sentinel.finance.security.GlobalExceptionHandler;
import com.sentinel.finance.security.JwtRolesConverter;
import com.sentinel.finance.security.ProblemDetailAccessDeniedHandler;
import com.sentinel.finance.security.ProblemDetailAuthenticationEntryPoint;
import com.sentinel.finance.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FinanceController.class)
@Import({SecurityConfig.class, JwtRolesConverter.class, ProblemDetailAuthenticationEntryPoint.class, 
         ProblemDetailAccessDeniedHandler.class, GlobalExceptionHandler.class, CorrelationIdFilter.class})
class FinanceControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void whenNoToken_then401ProblemDetail() throws Exception {
        mockMvc.perform(get("/api/finance/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Unauthorized"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void whenTokenWithoutFinanceAdmins_then403ProblemDetailOnAdminEndpoint() throws Exception {
        mockMvc.perform(get("/api/finance/admin/salaries")
                        .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_employees"))
                                .jwt(jwt -> jwt.claim("groups", java.util.List.of("employees")))))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Forbidden"))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void whenTokenWithFinanceAdmins_then200OnAdminEndpoint() throws Exception {
        mockMvc.perform(get("/api/finance/admin/salaries")
                        .with(jwt().authorities(
                                new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_employees"),
                                new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_finance-admins")
                        ).jwt(jwt -> jwt.claim("groups", java.util.List.of("employees", "finance-admins")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].employee").value("shree.admin"));
    }

    @Test
    void whenTokenWithEmployees_then200OnMeEndpoint() throws Exception {
        mockMvc.perform(get("/api/finance/me")
                        .with(jwt().authorities(
                                new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_employees")
                        ).jwt(jwt -> jwt
                                .claim("groups", java.util.List.of("employees"))
                                .claim("preferred_username", "testuser")
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_employees"));
    }
}
