package com.kaushik.apigateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class AuthenticationFilterGatewayFilterFactory
        extends AbstractGatewayFilterFactory<AuthenticationFilterGatewayFilterFactory.Config> {

    private static final Logger log =
            LoggerFactory.getLogger(AuthenticationFilterGatewayFilterFactory.class);
    private final WebClient.Builder webClientBuilder;

    @Value("${auth.service.url:${AUTH_SERVICE_URL:http://auth-service:4005}}")
    private String authServiceUrl;

    public AuthenticationFilterGatewayFilterFactory(WebClient.Builder webClientBuilder) {
        super(Config.class);
        this.webClientBuilder = webClientBuilder;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String authHeader =
                    exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader == null) {
                log.warn("Missing Authorization header");
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            if (authHeader.startsWith("Bearer ")) {
                String validateUri = authServiceUrl.replaceAll("/+$", "") + "/validate";
                return webClientBuilder
                        .build()
                        .post()
                        .uri(validateUri)
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .retrieve()
                        .toBodilessEntity()
                        .flatMap(
                                response -> {
                                    if (response.getStatusCode().is2xxSuccessful()) {
                                        log.info("Token validated successfully");
                                        return chain.filter(exchange);
                                    } else {
                                        log.warn(
                                                "Token validation failed with status: {}",
                                                response.getStatusCode());
                                        exchange.getResponse()
                                                .setStatusCode(HttpStatus.UNAUTHORIZED);
                                        return exchange.getResponse().setComplete();
                                    }
                                })
                        .onErrorResume(
                                e -> {
                                    log.error("Error occurred while validating token", e);
                                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                                    return exchange.getResponse().setComplete();
                                });
            } else {
                log.warn("Invalid Authorization header format");
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }
        };
    }

    public static class Config {
        // Put the configuration properties here if needed
    }
}
