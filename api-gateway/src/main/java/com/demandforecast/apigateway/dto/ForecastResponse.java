package com.demandforecast.apigateway.dto;

// クライアントへ返すレスポンスの形。
// predictedQuantity は Jackson によって JSON の "predictedQuantity" に変換される。
public class ForecastResponse {

    private double predictedQuantity;

    // Jackson がシリアライズ時に使用するコンストラクタ
    public ForecastResponse() {}

    public ForecastResponse(double predictedQuantity) {
        this.predictedQuantity = predictedQuantity;
    }

    public double getPredictedQuantity() { return predictedQuantity; }
    public void setPredictedQuantity(double predictedQuantity) {
        this.predictedQuantity = predictedQuantity;
    }
}
