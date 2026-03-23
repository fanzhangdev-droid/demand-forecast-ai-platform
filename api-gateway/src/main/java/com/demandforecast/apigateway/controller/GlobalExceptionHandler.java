package com.demandforecast.apigateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

// Bean Validation (@Valid) によるリクエスト検証エラーを一元処理するハンドラ。
// MethodArgumentNotValidException をキャッチし、フィールドエラーを読みやすいメッセージに変換して返す。
// 既存の forecastApi.js は errJson.error を読む実装になっているため、
// { "error": "..." } 形式で返すことで追加のフロントエンド変更なしに動作する。
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(e -> e.getDefaultMessage())
                .collect(Collectors.joining(" / "));
        return Map.of("error", message);
    }
}
