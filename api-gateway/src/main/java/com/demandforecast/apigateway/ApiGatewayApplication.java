package com.demandforecast.apigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Day 4: PostgreSQL を使用するため、JPA / DataSource の除外設定を削除した。
// Spring Boot が application.yml のデータソース設定を自動で読み込む。
@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
