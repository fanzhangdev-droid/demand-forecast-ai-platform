package com.demandforecast.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// ローカル開発環境用のグローバル CORS 設定クラス。
// フロントエンド（React）は Spring Boot とは異なるオリジンで動作するため、
// ブラウザの CORS ポリシーによってリクエストがブロックされる。
// このクラスで /api/** へのアクセスを明示的に許可することで問題を解消する。
@Configuration
public class WebConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry
                    // /api/ 以下のすべてのエンドポイントに CORS 設定を適用する
                    .addMapping("/api/**")
                    // Vite のデフォルトポート（5173）と、本プロジェクトが指定したポート（3000）を許可する
                    .allowedOrigins(
                        "http://localhost:3000",
                        "http://localhost:5173"
                    )
                    // プリフライトリクエスト（OPTIONS）を含む必要なメソッドをすべて許可する
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                    // Content-Type など、フロントエンドが送るすべてのリクエストヘッダーを許可する
                    .allowedHeaders("*");
            }
        };
    }
}
