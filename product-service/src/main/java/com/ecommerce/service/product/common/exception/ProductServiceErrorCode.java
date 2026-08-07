package com.ecommerce.service.product.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ProductServiceErrorCode {
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH_002", "Invalid email or password"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_003", "Token is invalid or expired"),
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "CMN_001", "Validation failed"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "CMN_002", "Forbidden"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "CMN_999", "Internal server error"),
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "CTG_001", "Category not found"),
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "PRD_001", "Product not found"),
    PRODUCT_SLUG_DUPLICATE(HttpStatus.CONFLICT, "PRD_002", "Slug duplicate"),
    INSUFFICIENT_STOCK(HttpStatus.CONFLICT, "PRD_003", "Insufficient stock quantity");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
