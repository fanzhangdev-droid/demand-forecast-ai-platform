package com.demandforecast.apigateway.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

// GET /api/forecast/history のレスポンス DTO。
// 予測履歴一覧をクライアントへ返すためのレスポンスDTO。
// Entity と API の返却形式を分離し、必要な項目のみ返却するために使用する。
public class HistoryResponse {

    private Long id;
    private LocalDate date;
    private String gender;
    private Integer age;
    private String productCategory;
    private Double pricePerUnit;
    private Double predictedQuantity;
    private LocalDateTime createdAt;

    // Jackson などの処理で利用されるデフォルトコンストラクタ
    public HistoryResponse() {}

    // Service 層での変換時に使うコンストラクタ
    public HistoryResponse(Long id, LocalDate date, String gender, Integer age,
                            String productCategory, Double pricePerUnit,
                            Double predictedQuantity, LocalDateTime createdAt) {
        this.id = id;
        this.date = date;
        this.gender = gender;
        this.age = age;
        this.productCategory = productCategory;
        this.pricePerUnit = pricePerUnit;
        this.predictedQuantity = predictedQuantity;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public LocalDate getDate() { return date; }
    public String getGender() { return gender; }
    public Integer getAge() { return age; }
    public String getProductCategory() { return productCategory; }
    public Double getPricePerUnit() { return pricePerUnit; }
    public Double getPredictedQuantity() { return predictedQuantity; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
