package com.javaeasybank.creditcard.service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.javaeasybank.common.config.LinePayProperties;
import com.javaeasybank.common.exception.BusinessException;
import com.javaeasybank.common.util.LinePaySignatureUtil;
import com.javaeasybank.creditcard.dto.CardTxnRequestDto;
import com.javaeasybank.creditcard.dto.LinePayRequestDto;
import com.javaeasybank.creditcard.enums.TransactionChannel;
import com.javaeasybank.creditcard.enums.TxnType;

import lombok.RequiredArgsConstructor;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class LinePayService {

        private final LinePayProperties linePayProperties;
        private final CardTxnService cardTxnService;
        private final RestTemplate restTemplate;
        private final ObjectMapper objectMapper;
        private final Map<String, LinePayRequestDto> orderStorage = new ConcurrentHashMap<>();

        @SuppressWarnings("unchecked")
        public Map<String, Object> request(LinePayRequestDto request) {
                String orderId = UUID.randomUUID().toString();
                orderStorage.put(orderId, request);
                String requestUri = "/v3/payments/request";
                String nonce = UUID.randomUUID().toString();
                String txnDescription = request.getDescription();

                //防止description空白
                if (txnDescription == null || txnDescription.isBlank()) {
                        txnDescription = "LINE Pay Transaction";
                }

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("X-LINE-ChannelId", linePayProperties.getChannelId());
                headers.set("X-LINE-Authorization-Nonce", nonce);
                Map<String, Object> body = Map.of("amount", request.getAmount(), "currency", "TWD", "orderId", orderId,
                                "packages",
                                new Object[] { Map.of("id", "1", "amount", request.getAmount(), "name",
                                                request.getDescription(), "products",
                                                new Object[] { Map.of("name", request.getDescription(), "quantity", 1,
                                                                "price", request.getAmount()) }) },
                                "redirectUrls",
                                Map.of(
                                                "confirmUrl",
                                                linePayProperties.getConfirmUrl(),

                                                "cancelUrl",
                                                linePayProperties.getCancelUrl()));
                String requestBody;
                try {
                        requestBody = objectMapper.writeValueAsString(body);
                } catch (Exception e) {
                        throw new RuntimeException(e);
                }
                String signature = LinePaySignatureUtil.generateSignature(linePayProperties.getChannelSecret(),
                                requestUri, requestBody, nonce);
                headers.set("X-LINE-Authorization", signature);
                HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
                ResponseEntity<Map<String, Object>> response = restTemplate.postForEntity(
                                linePayProperties.getApiBase() + requestUri, entity,
                                (Class<Map<String, Object>>) (Class<?>) Map.class);
                Map<String, Object> responseBody = response.getBody();
                if (responseBody == null) {
                        throw new BusinessException("LINE Pay request failed.");
                }
                Map<String, Object> result = new HashMap<>(responseBody);
                result.put("orderId", orderId);
                return result;
        }

        @SuppressWarnings("unchecked")
        public String confirm(String transactionId, String orderId) {

                LinePayRequestDto linePayRequestDto = orderStorage.get(orderId);
                if (linePayRequestDto == null) {
                        throw new BusinessException("Order not found.");
                }
                String requestUri = "/v3/payments/" + transactionId + "/confirm";
                Map<String, Object> body = Map.of("amount", linePayRequestDto.getAmount(), "currency", "TWD");
                String requestBody;
                try {
                        requestBody = objectMapper.writeValueAsString(body);
                } catch (Exception e) {
                        throw new RuntimeException(e);
                }
                String nonce = UUID.randomUUID().toString();
                String signature = LinePaySignatureUtil.generateSignature(linePayProperties.getChannelSecret(),
                                requestUri, requestBody, nonce);
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("X-LINE-ChannelId", linePayProperties.getChannelId());
                headers.set("X-LINE-Authorization-Nonce", nonce);
                headers.set("X-LINE-Authorization", signature);
                HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
                ResponseEntity<Map<String, Object>> response = restTemplate.postForEntity(
                                linePayProperties.getApiBase() + requestUri, entity,
                                (Class<Map<String, Object>>) (Class<?>) Map.class);
                Map<String, Object> responseBody = response.getBody();
                if (responseBody == null) {
                        throw new BusinessException("LINE Pay confirm failed.");
                }
                String returnCode = (String) responseBody.get("returnCode");
                if (!"0000".equals(returnCode)) {
                        throw new BusinessException("LINE Pay confirm failed.");
                }
                CardTxnRequestDto dto = new CardTxnRequestDto();
                dto.setTxnAmount(linePayRequestDto.getAmount());
                dto.setTxnType(TxnType.PURCHASE);
                dto.setDescription(linePayRequestDto.getDescription());
                dto.setCardId(linePayRequestDto.getCardId());
                dto.setMerchantId(linePayRequestDto.getMerchantId());
                dto.setChannel(TransactionChannel.LINE_PAY);
                dto.setExternalTxnId(transactionId);
                cardTxnService.create(dto);
                orderStorage.remove(orderId);
                return "付款成功";
        }

}
