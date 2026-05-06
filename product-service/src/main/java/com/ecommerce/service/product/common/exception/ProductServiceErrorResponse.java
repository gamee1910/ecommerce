package com.ecommerce.service.product.common.exception;

import com.ecommerce.service.product.common.utils.TimeUtils;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProductServiceErrorResponse(String code, String message, String timestamp, Map<String, String> errors) {

    public static ProductServiceErrorResponse of(ProductServiceErrorCode errorCode) {
        return new ProductServiceErrorResponse(
                errorCode.getCode(), errorCode.getMessage(), TimeUtils.formatVn(Instant.now()), null);
    }

    public static ProductServiceErrorResponse of(ProductServiceErrorCode errorCode, Map<String, String> errors) {
        return new ProductServiceErrorResponse(
                errorCode.getCode(), errorCode.getMessage(), TimeUtils.formatVn(Instant.now()), errors);
    }
}
