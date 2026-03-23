package com.demandforecast.apigateway.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

// @Entity: このクラスが JPA で管理されるデータベースのテーブルと対応することを宣言する
@Entity
// @Table: 対応するテーブル名を "prediction_history" に指定する
@Table(name = "prediction_history")
public class PredictionHistory {

    // @Id: このフィールドが主キーであることを示す
    // @GeneratedValue: INSERT 時に DB 側で自動採番（AUTO INCREMENT）する
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 予測対象の日付
    @Column(name = "date", nullable = false)
    private LocalDate date;

    // 顧客の性別
    @Column(name = "gender", nullable = false)
    private String gender;

    // 顧客の年齢
    @Column(name = "age", nullable = false)
    private Integer age;

    // 商品カテゴリ（DB では snake_case のカラム名を使う）
    @Column(name = "product_category", nullable = false)
    private String productCategory;

    // 単価（DB では snake_case のカラム名を使う）
    @Column(name = "price_per_unit", nullable = false)
    private Double pricePerUnit;

    // ML サービスが返した予測需要数量
    @Column(name = "predicted_quantity", nullable = false)
    private Double predictedQuantity;

    // レコードの作成日時
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // デフォルトコンストラクタ（JPA が内部で使用するため必須）
    public PredictionHistory() {}

    public Long getId() { return id; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }

    public String getProductCategory() { return productCategory; }
    public void setProductCategory(String productCategory) { this.productCategory = productCategory; }

    public Double getPricePerUnit() { return pricePerUnit; }
    public void setPricePerUnit(Double pricePerUnit) { this.pricePerUnit = pricePerUnit; }

    public Double getPredictedQuantity() { return predictedQuantity; }
    public void setPredictedQuantity(Double predictedQuantity) { this.predictedQuantity = predictedQuantity; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
