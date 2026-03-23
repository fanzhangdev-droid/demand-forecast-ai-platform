package com.demandforecast.apigateway.client;

import com.demandforecast.apigateway.dto.MlPredictRequest;
import com.demandforecast.apigateway.dto.MlPredictResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

// ML サービス（FastAPI）への HTTP 呼び出しを担当するクラス。
// HTTP の詳細（URL の組み立て、RestTemplate の使い方）をここに閉じ込め、
// Service 層がこれを意識しなくて済むようにする。
@Component
public class MlServiceClient {

    private final RestTemplate restTemplate;

    // application.yml の ml.service.base-url を読み込む
    private final String mlServiceBaseUrl;

    public MlServiceClient(RestTemplate restTemplate,
                            @Value("${ml.service.base-url}") String mlServiceBaseUrl) {
        this.restTemplate = restTemplate;
        this.mlServiceBaseUrl = mlServiceBaseUrl;
    }

    // FastAPI の /predict エンドポイントを呼び出し、予測結果を返す
    public MlPredictResponse predict(MlPredictRequest request) {
        String url = mlServiceBaseUrl + "/predict";

        try {
            // POST リクエストを送り、レスポンスを MlPredictResponse にデシリアライズする
            return restTemplate.postForObject(url, request, MlPredictResponse.class);
        } catch (Exception e) {
            // ML サービスが落ちている場合や通信エラーの場合は RuntimeException に包んで上に伝える
            throw new RuntimeException("ML サービスへの呼び出しに失敗しました: " + e.getMessage(), e);
        }
    }
}
