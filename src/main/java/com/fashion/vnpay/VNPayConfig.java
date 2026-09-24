package com.fashion.vnpay;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
public class VNPayConfig {

    public static String vnp_PayUrl;
    public static String vnp_Returnurl;
    public static String vnp_TmnCode;
    public static String vnp_HashSecret;
    public static String vnp_apiUrl;

    @Value("${vnpay.pay-url}")
    public void setVnpPayUrl(String value) {
        vnp_PayUrl = value;
    }

    @Value("${vnpay.return-url}")
    public void setVnpReturnurl(String value) {
                if (value == null || value.isBlank() || value.contains("<") || value.contains(">")) {
                        vnp_Returnurl = "http://localhost:8081/api/vnpay/return";
                        return;
                }
                vnp_Returnurl = value;
    }

    @Value("${vnpay.tmn-code}")
    public void setVnpTmnCode(String value) {
        vnp_TmnCode = value;
    }

    @Value("${vnpay.hash-secret}")
    public void setVnpHashSecret(String value) {
        vnp_HashSecret = value;
    }

    @Value("${vnpay.api-url}")
    public void setVnpApiUrl(String value) {
        vnp_apiUrl = value;
    }

    public static String hashAllFields(Map<String, String> fields) {
        List<String> fieldNames = new ArrayList<>(fields.keySet());
        Collections.sort(fieldNames);

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < fieldNames.size(); i++) {
            String fieldName = fieldNames.get(i);
            String fieldValue = fields.get(fieldName);

            if (fieldValue != null && !fieldValue.isEmpty()) {
                sb.append(fieldName)
                  .append("=")
                  .append(fieldValue);
            }

            if (i < fieldNames.size() - 1) {
                sb.append("&");
            }
        }

        return hmacSHA512(
                vnp_HashSecret,
                sb.toString()
        );
    }

    public static String hmacSHA512(
            final String key,
            final String data
    ) {
        try {
            if (key == null || data == null) {
                throw new IllegalArgumentException(
                        "VNPay key/data must not be null"
                );
            }

            Mac hmac512 = Mac.getInstance("HmacSHA512");

            SecretKeySpec secretKey =
                    new SecretKeySpec(
                            key.getBytes(StandardCharsets.UTF_8),
                            "HmacSHA512"
                    );

            hmac512.init(secretKey);

            byte[] result =
                    hmac512.doFinal(
                            data.getBytes(StandardCharsets.UTF_8)
                    );

            StringBuilder sb =
                    new StringBuilder(
                            2 * result.length
                    );

            for (byte b : result) {
                sb.append(
                        String.format(
                                "%02x",
                                b & 0xff
                        )
                );
            }

            return sb.toString();

        } catch (Exception ex) {
            throw new RuntimeException(
                    "Cannot create VNPay HMAC SHA512",
                    ex
            );
        }
    }

    public static String getIpAddress(
            HttpServletRequest request
    ) {
        try {
            String ipAddress =
                    request.getHeader(
                            "X-Forwarded-For"
                    );

            if (
                ipAddress == null ||
                ipAddress.isBlank()
            ) {
                ipAddress =
                        request.getRemoteAddr();
            } else {
                /*
                 * Nếu đi qua proxy có thể trả:
                 * clientIP, proxy1, proxy2
                 */
                ipAddress =
                        ipAddress
                                .split(",")[0]
                                .trim();
            }

                    if ("::1".equals(ipAddress)
                            || "0:0:0:0:0:0:0:1".equals(ipAddress)) {
                        ipAddress = "127.0.0.1";
                    }

            return ipAddress;

        } catch (Exception e) {
            return "127.0.0.1";
        }
    }

    public static String getRandomNumber(int len) {
        Random rnd = new Random();

        String chars =
                "0123456789";

        StringBuilder sb =
                new StringBuilder(len);

        for (int i = 0; i < len; i++) {
            sb.append(
                    chars.charAt(
                            rnd.nextInt(
                                    chars.length()
                            )
                    )
            );
        }

        return sb.toString();
    }
}