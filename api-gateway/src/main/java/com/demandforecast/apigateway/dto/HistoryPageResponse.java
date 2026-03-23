package com.demandforecast.apigateway.dto;

import org.springframework.data.domain.Page;

import java.util.List;

// GET /api/forecast/history のページング対応レスポンス DTO。
// Page<HistoryResponse> を受け取り、フロントエンドが必要とするフィールドのみ公開する。
// Spring の Page 型をそのままシリアライズすると不要なフィールドが多いため、専用 DTO に変換する。
public class HistoryPageResponse {

    private List<HistoryResponse> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean hasNext;
    private boolean hasPrevious;

    // Jackson 用デフォルトコンストラクタ
    public HistoryPageResponse() {}

    // Page<HistoryResponse> から変換するファクトリメソッド。
    // Service 層はこのメソッドを呼ぶだけでよく、ページング詳細を意識しなくてよい。
    public static HistoryPageResponse from(Page<HistoryResponse> page) {
        HistoryPageResponse r = new HistoryPageResponse();
        r.content = page.getContent();
        r.page = page.getNumber();
        r.size = page.getSize();
        r.totalElements = page.getTotalElements();
        r.totalPages = page.getTotalPages();
        r.hasNext = page.hasNext();
        r.hasPrevious = page.hasPrevious();
        return r;
    }

    public List<HistoryResponse> getContent() { return content; }
    public int getPage() { return page; }
    public int getSize() { return size; }
    public long getTotalElements() { return totalElements; }
    public int getTotalPages() { return totalPages; }
    public boolean isHasNext() { return hasNext; }
    public boolean isHasPrevious() { return hasPrevious; }
}
