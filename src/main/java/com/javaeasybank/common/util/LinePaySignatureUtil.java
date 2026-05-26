package com.javaeasybank.common.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;


public class LinePaySignatureUtil {

    private static final String HMAC_SHA256 = "HmacSHA256";

    public static String generateSignature(
            String channelSecret,
            String requestUri,
            String requestBody,
            String nonce
    ) {

        try {

            String message =
                    channelSecret
                    + requestUri
                    + requestBody
                    + nonce;

            SecretKeySpec signingKey =
                    new SecretKeySpec(
                            channelSecret.getBytes(StandardCharsets.UTF_8),
                            HMAC_SHA256
                    );

            Mac mac = Mac.getInstance(HMAC_SHA256);

            mac.init(signingKey);

            byte[] rawHmac =
                    mac.doFinal(
                            message.getBytes(StandardCharsets.UTF_8)
                    );

            return Base64.getEncoder()
                    .encodeToString(rawHmac);

        } catch (Exception e) {

            throw new RuntimeException(e);
        }
    }
    
}
