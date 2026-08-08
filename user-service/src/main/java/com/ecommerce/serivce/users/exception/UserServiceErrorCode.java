package com.ecommerce.serivce.users.exception;

import com.gamee1910.error.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserServiceErrorCode implements ErrorCode {
    EMAIL_ALREADY_EXIST(HttpStatus.CONFLICT, "AUTH_001", "Email already registered"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH_002", "Invalid email or password"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_003", "Token is invalid or expired"),
    ACCOUNT_DISABLED(HttpStatus.FORBIDDEN, "AUTH_004", "Account is disabled"),
    EXPIRED_TOKEN(HttpStatus.CONFLICT, "AUTH_005", "Token expired"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_001", "User not found"),
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "CMN_001", "Validation failed"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "CMN_002", "Forbidden"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "CMN_999", "Internal server error");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
