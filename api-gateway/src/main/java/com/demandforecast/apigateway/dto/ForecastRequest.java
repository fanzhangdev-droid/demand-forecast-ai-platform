package com.demandforecast.apigateway.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

// フロントエンドから受け取るリクエストの形。
// フロントエンドは snake_case（product_category / price_per_unit）で送ってくるため、
// @JsonProperty で JSON キー名を明示的に指定して Jackson に正しくマッピングさせる。
public class ForecastRequest {

    @NotBlank(message = "date は必須です")
    private String date;

    @NotBlank(message = "gender は必須です")
    private String gender;

    @Min(value = 1,   message = "age は 1 以上である必要があります")
    @Max(value = 120, message = "age は 120 以下である必要があります")
    private int age;

    // フロントエンドが送る JSON キーは "product_category"（snake_case）
    // @JsonProperty を付けることで Jackson がこのキーを productCategory フィールドに紐づける
    @NotBlank(message = "productCategory は必須です")
    @JsonProperty("product_category")
    private String productCategory;

    // フロントエンドが送る JSON キーは "price_per_unit"（snake_case）
    @DecimalMin(value = "0.0", inclusive = false, message = "pricePerUnit は 0 より大きい値を入力してください")
    @JsonProperty("price_per_unit")
    private double pricePerUnit;

    // デフォルトコンストラクタ（Jackson がデシリアライズ時に使用する）
    public ForecastRequest() {}

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
