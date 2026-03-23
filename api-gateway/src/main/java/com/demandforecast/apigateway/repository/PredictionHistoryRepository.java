package com.demandforecast.apigateway.repository;

import com.demandforecast.apigateway.entity.PredictionHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

// JpaRepository を継承することで、基本的な CRUD 操作が自動実装される。
// 第1型引数: 操作対象のエンティティクラス
// 第2型引数: 主キーの型
public interface PredictionHistoryRepository extends JpaRepository<PredictionHistory, Long> {

    // ── 非ページング版（既存） ─────────────────────────────────────────────
    List<PredictionHistory> findAllByOrderByCreatedAtDesc();
    List<PredictionHistory> findAllByProductCategoryOrderByCreatedAtDesc(String productCategory);
    List<PredictionHistory> findAllByDateOrderByCreatedAtDesc(LocalDate date);
    List<PredictionHistory> findAllByProductCategoryAndDateOrderByCreatedAtDesc(
            String productCategory, LocalDate date);

    // ── ページング版 ─────────────────────────────────────────────────────
    // Pageable に Sort.by(DESC, "createdAt") を渡すことでメソッド名の OrderBy と同じ降順が保証される。
    Page<PredictionHistory> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<PredictionHistory> findByProductCategoryOrderByCreatedAtDesc(String productCategory, Pageable pageable);
    Page<PredictionHistory> findByDateOrderByCreatedAtDesc(LocalDate date, Pageable pageable);
    Page<PredictionHistory> findByProductCategoryAndDateOrderByCreatedAtDesc(
            String productCategory, LocalDate date, Pageable pageable);
}
