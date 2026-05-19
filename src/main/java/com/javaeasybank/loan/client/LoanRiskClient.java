package com.javaeasybank.loan.client;

import com.javaeasybank.common.exception.BusinessException;
import com.javaeasybank.loan.dto.requests.LoanDocumentInfoDTO;
import com.javaeasybank.loan.dto.requests.LoanRiskRequestDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * 風控模組 HTTP 客戶端。
 *
 * <p>封裝所有與外部風控系統的 HTTP 通訊，
 * 使用 {@code RestTemplate} 呼叫風控 API，
 * 讓 Service 層無需關注底層請求細節。</p>
 *
 * <p><b>設計決策：</b>{@code submitForReview} 若風控服務不可達，
 * 會拋出 {@code BusinessException}，
 * 使外層 {@code @Transactional} 回滾整筆送審操作，
 * 避免申請狀態停留在 {@code PENDING_REVIEW} 卻未實際送出。</p>
 *
 * <p><b>設定項目</b>（{@code application-local.properties}）：</p>
 * <pre>
 * risk.api.base-url=http://localhost:8080/api/risk/
 * risk.api.loan.callback-url=http://localhost:8080/api/loan-callbacks
 * </pre>
 */
@Slf4j
@Component
public class LoanRiskClient {

    /**
     * 風控 API 的 base URL，例如 {@code http://risk-service/api/risk/}。
     * 從 {@code application-local.properties} 的 {@code risk.api.base-url} 注入。
     */
    @Value("${risk.api.base-url}")
    private String riskBaseUrl;

    /**
     * 風控完成審核後回呼的 base URL。
     * 優先讀取 {@code risk.api.loan.callback-url}；
     * 不存在則 fallback 至 {@code risk.api.callback-url}；
     * 兩者皆無則預設為 {@code http://localhost:8080/api/loan-callbacks}。
     */
    @Value("${risk.api.loan.callback-url:${risk.api.callback-url:http://localhost:8080/api/loan-callbacks}}")
    private String callbackBaseUrl;

    private final RestTemplate restTemplate;

    public LoanRiskClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 補件通知：將客戶新上傳的文件清單推送給風控，更新對應的審核任務（ReviewTask）。
     *
     * <p>此操作為 best-effort：若風控服務不可達，僅記錄警告日誌，
     * 不拋出例外、不影響補件主流程，風控下次主動拉取時仍可取得最新文件。</p>
     *
     * @param businessId 業務單號（對應 {@code applicationId}）
     * @param documents  此次補件的文件清單
     */
    public void attachDocuments(String businessId, List<LoanDocumentInfoDTO> documents) {
        String url = buildRiskUrl("reviews/" + businessId + "/attachments");
        log.info("[RiskClient] 補件通知 businessId={} count={} → {}", businessId, documents.size(), url);
        try {
            Map<String, Object> body = Map.of("documents", documents);
            restTemplate.exchange(url, HttpMethod.PATCH, new HttpEntity<>(body), Void.class);
        } catch (RestClientException e) {
            // 補件通知失敗不影響主流程，僅記錄警告
            log.warn("[RiskClient] 補件通知風控失敗 businessId={} err={}", businessId, e.getMessage());
        }
    }

    /**
     * 送審：將整合後的審核資料 POST 至風控系統的 {@code /risk/reviews} 入口。
     *
     * <p>呼叫前會自動注入 {@code callbackUrl}，風控審核完成後將透過此 URL 回呼
     * {@code LoanCallbackController} 更新申請狀態。</p>
     *
     * @param dto 整合申請與二次填單的審核請求 DTO
     * @throws BusinessException 若風控服務不可達或回應非 2xx，拋出例外觸發事務回滾
     */
    public void submitForReview(LoanRiskRequestDTO dto) {
        dto.setCallbackUrl(callbackBaseUrl + "/" + dto.getApplicationId() + "/status");

        String url = buildRiskUrl("reviews");
        log.info("[RiskClient] 送審 applicationId={} → {}", dto.getApplicationId(), url);

        try {
            ResponseEntity<Void> response = restTemplate.postForEntity(url, dto, Void.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new BusinessException("風控模組回應異常，HTTP " + response.getStatusCode().value());
            }
        } catch (RestClientException e) {
            log.error("[RiskClient] 呼叫風控失敗 applicationId={}", dto.getApplicationId(), e);
            throw new BusinessException("送審失敗：無法連接風控模組，請稍後再試");
        }
    }

    /**
     * 組裝完整的風控 API URL。
     * 確保 {@code riskBaseUrl} 末尾有斜線，再接上指定的路徑。
     *
     * @param path 相對路徑，例如 {@code "reviews"} 或 {@code "reviews/abc/attachments"}
     * @return 完整的風控 API URL
     */
    private String buildRiskUrl(String path) {
        String normalizedBaseUrl = riskBaseUrl.endsWith("/") ? riskBaseUrl : riskBaseUrl + "/";
        return normalizedBaseUrl + path;
    }
}
