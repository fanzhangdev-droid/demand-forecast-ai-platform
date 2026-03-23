package com.demandforecast.apigateway.service;

import com.demandforecast.apigateway.client.MlServiceClient;
import com.demandforecast.apigateway.dto.ForecastRequest;
import com.demandforecast.apigateway.dto.ForecastResponse;
import com.demandforecast.apigateway.dto.HistoryPageResponse;
import com.demandforecast.apigateway.dto.HistoryResponse;
import com.demandforecast.apigateway.dto.MlPredictRequest;
import com.demandforecast.apigateway.dto.MlPredictResponse;
import com.demandforecast.apigateway.entity.PredictionHistory;
import com.demandforecast.apigateway.repository.PredictionHistoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// 予測処理と予測履歴の保存・取得を担当するサービスクラス。
@Service
public class ForecastService {

    private final MlServiceClient mlServiceClient;
    private final PredictionHistoryRepository predictionHistoryRepository;

    // コンストラクタインジェクションで依存オブジェクトを受け取る。
    public ForecastService(MlServiceClient mlServiceClient,
                           PredictionHistoryRepository predictionHistoryRepository) {
        this.mlServiceClient = mlServiceClient;
        this.predictionHistoryRepository = predictionHistoryRepository;
    }

    // 予測を実行し、結果をDBへ保存してクライアントへ返す。
    public ForecastResponse forecast(ForecastRequest request) {
        MlPredictRequest mlRequest = new MlPredictRequest(
                request.getDate(),
                request.getGender(),
                request.getAge(),
                request.getProductCategory(),
                request.getPricePerUnit()
        );

        MlPredictResponse mlResponse = mlServiceClient.predict(mlRequest);
        double predictedQuantity = mlResponse.getPredictedQuantity();

        PredictionHistory history = new PredictionHistory();
        history.setDate(LocalDate.parse(request.getDate()));
        history.setGender(request.getGender());
        history.setAge(request.getAge());
        history.setProductCategory(request.getProductCategory());
        history.setPricePerUnit(request.getPricePerUnit());
        history.setPredictedQuantity(predictedQuantity);
        history.setCreatedAt(LocalDateTime.now());

        predictionHistoryRepository.save(history);

        return new ForecastResponse(predictedQuantity);
    }

    // 予測履歴をフィルター + ページング条件付きで取得する。
    // productCategory / date はどちらも省略可能（null の場合はそのフィルターを無視する）。
    // page / size の不正値は自動修正し、例外は返さない。
    public HistoryPageResponse getPredictionHistory(
            String productCategory, LocalDate date, int page, int size) {

        // 不正値を自動修正する（例外を返すより UX が良い）
        page = Math.max(0, page);
        size = Math.min(50, Math.max(1, size));

        // Sort はメソッド名の OrderBy と同じ条件を明示的に指定する
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<PredictionHistory> rows;
        if (productCategory != null && date != null) {
            rows = predictionHistoryRepository
                    .findByProductCategoryAndDateOrderByCreatedAtDesc(productCategory, date, pageable);
        } else if (productCategory != null) {
            rows = predictionHistoryRepository
                    .findByProductCategoryOrderByCreatedAtDesc(productCategory, pageable);
        } else if (date != null) {
            rows = predictionHistoryRepository
                    .findByDateOrderByCreatedAtDesc(date, pageable);
        } else {
            rows = predictionHistoryRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        Page<HistoryResponse> dtoPage = rows.map(this::toHistoryResponse);
        return HistoryPageResponse.from(dtoPage);
    }

    // Entity をレスポンスDTOへ変換する。
    private HistoryResponse toHistoryResponse(PredictionHistory entity) {
        return new HistoryResponse(
                entity.getId(),
                entity.getDate(),
                entity.getGender(),
                entity.getAge(),
                entity.getProductCategory(),
                entity.getPricePerUnit(),
                entity.getPredictedQuantity(),
                entity.getCreatedAt()
        );
    }
}