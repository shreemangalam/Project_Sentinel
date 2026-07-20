package com.sentinel.gateway.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.client.oidc.web.server.logout.OidcClientInitiatedServerLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationEntryPoint;
import org.springframework.web.server.session.CookieWebSessionIdResolver;
import org.springframework.web.server.session.WebSessionIdResolver;
import reactor.core.publisher.Mono;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.Instant;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.slf4j.MDC;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {


    private final RedirectServerAuthenticationEntryPoint defaultEntryPoint = 
        new RedirectServerAuthenticationEntryPoint("/oauth2/authorization/keycloak");

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http, 
                                                            ReactiveClientRegistrationRepository clientRegistrationRepository,
                                                            ObjectMapper objectMapper) {
        
        OidcClientInitiatedServerLogoutSuccessHandler logoutSuccessHandler = 
            new OidcClientInitiatedServerLogoutSuccessHandler(clientRegistrationRepository);
        logoutSuccessHandler.setPostLogoutRedirectUri("{baseUrl}/");

        http
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers("/actuator/health", "/actuator/info").permitAll()
                .anyExchange().authenticated()
            )
            .exceptionHandling(exceptionHandling -> exceptionHandling
                .authenticationEntryPoint(customAuthenticationEntryPoint(objectMapper))
            )
            .oauth2Login(oauth2 -> {}) // default configuration
            .logout(logout -> logout
                .logoutSuccessHandler(logoutSuccessHandler)
            )
            .csrf(csrf -> csrf.disable()); // CSRF is mitigated by SameSite=Lax and no direct mutative endpoints on gateway itself

        return http.build();
    }

    @Bean
    public WebSessionIdResolver webSessionIdResolver() {
        CookieWebSessionIdResolver resolver = new CookieWebSessionIdResolver();
        resolver.setCookieName("SENTINEL_SESSION");
        // Spring Session defaults to HttpOnly=true and SameSite=Lax, but we can enforce it if needed.
        resolver.addCookieInitializer(builder -> builder.sameSite("Lax").httpOnly(true));
        return resolver;
    }

    private ServerAuthenticationEntryPoint customAuthenticationEntryPoint(ObjectMapper objectMapper) {
        return (exchange, ex) -> {
            HttpHeaders headers = exchange.getRequest().getHeaders();
            boolean isXhr = "XMLHttpRequest".equals(headers.getFirst("X-Requested-With")) ||
                            (headers.getAccept().contains(MediaType.APPLICATION_JSON) && 
                             !headers.getAccept().contains(MediaType.TEXT_HTML));

            if (isXhr) {
                // Return 401 Problem Detail for API clients
                ServerHttpResponse response = exchange.getResponse();
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                response.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);

                ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Authentication is required");
                problem.setType(URI.create("urn:sentinel:unauthorized"));
                problem.setTitle("Unauthorized");
                problem.setProperty("timestamp", Instant.now());
                
                String correlationId = exchange.getAttribute("X-Correlation-Id");
                if (correlationId != null) {
                    problem.setProperty("correlation_id", correlationId);
                }

                return response.writeWith(Mono.fromSupplier(() -> {
                    DataBufferFactory bufferFactory = response.bufferFactory();
                    try {
                        byte[] bytes = objectMapper.writeValueAsBytes(problem);
                        return bufferFactory.wrap(bytes);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return bufferFactory.wrap(new byte[0]);
                    }
                }));
            } else {
                // Return 302 Redirect to Keycloak for browsers
                return defaultEntryPoint.commence(exchange, ex);
            }
        };
    }
}
