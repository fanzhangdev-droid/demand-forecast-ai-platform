package com.demandforecast.apigateway.controller;

import com.demandforecast.apigateway.dto.ApiResponse;
import com.demandforecast.apigateway.dto.ForecastRequest;
import com.demandforecast.apigateway.dto.ForecastResponse;
import com.demandforecast.apigateway.dto.HistoryPageResponse;
import com.demandforecast.apigateway.service.ForecastService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

// HTTP リクエストを受け取り、Service に処理を委譲するコントローラ。
// Day 6 から全エンドポイントの返却形式を ApiResponse<T> に統一した。
// Service 層が返す型はそのままで、Controller でラップするだけ。
@RestController
@RequestMapping("/api")
public class ForecastController {

    private final ForecastService forecastService;

    public ForecastController(ForecastService forecastService) {
        this.forecastService = forecastService;
    }

    // GET /api/health
    // サービスの生存確認用エンドポイント。Service 層は不要。
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    // POST /api/forecast
    // 予測リクエストを受け取り、{ "success": true, "data": { "predictedQuantity": ... } } を返す。
    @PostMapping("/forecast")
    public ApiResponse<ForecastResponse> forecast(@Valid @RequestBody ForecastRequest request) {
        ForecastResponse result = forecastService.forecast(request);
        return ApiResponse.success(result);
    }

    // GET /api/forecast/history?productCategory=Beauty&date=2026-03-22&page=0&size=10
    // productCategory / date はどちらも省略可能。page / size は省略時にデフォルト値を使用する。
    // 不正な page / size 値は Service 層で自動修正（page>=0, 1<=size<=50）する。
    @GetMapping("/forecast/history")
    public ApiResponse<HistoryPageResponse> getHistory(
            @RequestParam(required = false) String productCategory,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(forecastService.getPredictionHistory(productCategory, date, page, size));
    }
}
