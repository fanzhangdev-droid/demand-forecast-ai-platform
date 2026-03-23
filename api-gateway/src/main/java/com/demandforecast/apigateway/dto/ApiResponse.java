package com.demandforecast.apigateway.dto;

// 全 API エンドポイントで共通して使う汎用レスポンスラッパー。
// フロントエンドが success フラグを見て処理を分岐できるようにするために使う。
//
// 使用例:
//   ApiResponse<ForecastResponse> r = ApiResponse.success(forecastResponse);
//   ApiResponse<List<HistoryResponse>> r = ApiResponse.success(historyList);
//
// T にはエンドポイントごとのデータ型を指定する。
public class ApiResponse<T> {

    // リクエストが成功したかどうかを示すフラグ
    private final boolean success;

    // 実際のレスポンスデータ。型はエンドポイントによって異なる。
    private final T data;

    // コンストラクタは private にして、ファクトリメソッド経由でのみインスタンス化させる
    private ApiResponse(boolean success, T data) {
        this.success = success;
        this.data = data;
    }

    // 成功レスポンスを生成するファクトリメソッド。Controller から呼び出す。
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data);
    }

    public boolean isSuccess() {
        return success;
    }

    public T getData() {
        return data;
    }
}
