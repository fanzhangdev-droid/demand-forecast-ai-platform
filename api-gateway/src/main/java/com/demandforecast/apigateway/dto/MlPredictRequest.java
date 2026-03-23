package com.demandforecast.apigateway.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

// FastAPI（ML サービス）へ送るリクエストの形。
// FastAPI は snake_case を期待するため、@JsonProperty でフィールド名を変換する。
// 例: Java の productCategory -> JSON の "product_category"
public class MlPredictRequest {

    private String date;
    private String gender;
    private int age;

    // JSON に書き出す際は "product_category" というキー名になる
    @JsonProperty("product_category")
    private String productCategory;

    // JSON に書き出す際は "price_per_unit" というキー名になる
    @JsonProperty("price_per_unit")
    private double pricePerUnit;

    public MlPredictRequest() {}

    public MlPredictRequest(String date, String gender, int age,
                             String productCategory, double pricePerUnit) {
        this.date = date;
        this.gender = gender;
        this.age = age;
        this.productCategory = productCategory;
        this.pricePerUnit = pricePerUnit;
    }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public String getProductCategory() { return productCategory; }
    public void setProductCategory(String productCategory) { this.productCategory = productCategory; }

    public double getPricePerUnit() { return pricePerUnit; }
    public void setPricePerUnit(double pricePerUnit) { this.pricePerUnit = pricePerUnit; }
}
