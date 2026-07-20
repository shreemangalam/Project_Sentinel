package com.sentinel.gateway.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import org.springframework.core.io.buffer.DataBufferFactory;
import java.net.URI;
import java.time.Instant;

@Component
@Order(-2)
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper;

    public GlobalExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }

        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        if (ex instanceof ResponseStatusException rse) {
            status = HttpStatus.valueOf(rse.getStatusCode().value());
        } else if (ex instanceof org.springframework.security.access.AccessDeniedException) {
            status = HttpStatus.FORBIDDEN;
        } else if (ex instanceof org.springframework.security.core.AuthenticationException) {
            status = HttpStatus.UNAUTHORIZED;
        }

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, ex.getMessage() != null ? ex.getMessage() : "Unknown error");
        problem.setType(URI.create("urn:sentinel:error"));
        problem.setTitle(status.getReasonPhrase());
        problem.setProperty("timestamp", Instant.now().toString());

        String correlationId = exchange.getAttribute("X-Correlation-Id");
        if (correlationId != null) {
            problem.setProperty("correlation_id", correlationId);
        }

        return exchange.getResponse().writeWith(Mono.fromSupplier(() -> {
            DataBufferFactory bufferFactory = exchange.getResponse().bufferFactory();
            try {
                return bufferFactory.wrap(objectMapper.writeValueAsBytes(problem));
            } catch (Exception e) {
                return bufferFactory.wrap(new byte[0]);
            }
        }));
    }
}
