package com.demandforecast.apigateway.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

// FastAPI（ML サービス）から受け取るレスポンスの形。
// FastAPI は snake_case で返すため、@JsonProperty でフィールド名を変換する。
// 例: JSON の "predicted_quantity" -> Java の predictedQuantity
public class MlPredictResponse {

    // JSON の "predicted_quantity" をこのフィールドにマッピングする
    @JsonProperty("predicted_quantity")
    private double predictedQuantity;

    public MlPredictResponse() {}

    public double getPredictedQuantity() { return predictedQuantity; }
    public void setPredictedQuantity(double predictedQuantity) {
        this.predictedQuantity = predictedQuantity;
    }
}
