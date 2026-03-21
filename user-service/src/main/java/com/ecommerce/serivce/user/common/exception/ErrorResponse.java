package com.ecommerce.serivce.user.common.exception;

import com.ecommerce.serivce.user.common.utils.TimeUtils;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(String code, String message, String timestamp, Map<String, String> errors) {

    public static ErrorResponse of(UserServiceErrorCode errorCode) {
        return new ErrorResponse(errorCode.getCode(), errorCode.getMessage(), TimeUtils.formatVn(Instant.now()), null);
    }

    public static ErrorResponse of(UserServiceErrorCode errorCode, Map<String, String> errors) {
        return new ErrorResponse(
                errorCode.getCode(), errorCode.getMessage(), TimeUtils.formatVn(Instant.now()), errors);
    }
}
