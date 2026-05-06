package com.ecommerce.service.product.common.exception;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j(topic = "ProductServiceExceptionHandler")
public class ProductServiceExceptionHandler {

    @ExceptionHandler(ProductServiceException.class)
    public ResponseEntity<ProductServiceErrorResponse> handleUserServiceException(ProductServiceException ex) {
        ProductServiceErrorCode errorCode = ex.getErrorCode();
        return ResponseEntity.status(errorCode.getHttpStatus()).body(ProductServiceErrorResponse.of(errorCode));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProductServiceErrorResponse> handlValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fieldError -> Objects.requireNonNullElse(fieldError.getDefaultMessage(), "Invalid value"),
                        (existingValue, newValue) -> existingValue));
        return ResponseEntity.status(ProductServiceErrorCode.VALIDATION_FAILED.getHttpStatus())
                .body(ProductServiceErrorResponse.of(ProductServiceErrorCode.VALIDATION_FAILED, errors));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProductServiceErrorResponse> handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(ProductServiceErrorCode.INTERNAL_ERROR.getHttpStatus())
                .body(ProductServiceErrorResponse.of(ProductServiceErrorCode.INTERNAL_ERROR));
    }
}
