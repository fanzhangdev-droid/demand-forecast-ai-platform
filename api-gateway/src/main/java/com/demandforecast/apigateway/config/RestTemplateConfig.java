package com.demandforecast.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    // RestTemplate を Spring Bean として登録する。
    // @Bean にすることで、他のクラスに DI（依存性注入）できるようになる。
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
