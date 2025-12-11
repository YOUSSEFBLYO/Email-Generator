package com.youssef.email.app;

import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import reactor.netty.http.client.HttpClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient.Builder webClientBuilder() {
        // Configure timeouts: 10 seconds for connection, read, write
        HttpClient httpClient = HttpClient.create()
            .responseTimeout(java.time.Duration.ofSeconds(10))
            .doOnConnected(connection -> {
                connection.addHandlerLast(new ReadTimeoutHandler(10, TimeUnit.SECONDS));
                connection.addHandlerLast(new WriteTimeoutHandler(10, TimeUnit.SECONDS));
            });
        
        return WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(httpClient));
    }
}
