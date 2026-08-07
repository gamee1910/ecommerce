package io.game.service.orders.common.exception;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j(topic = "GlobalExceptionHandler")
public class GlobalExceptionHandler {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @ExceptionHandler(OrderServiceException.class)
    public ResponseEntity<ErrorResponse> handleOrderServiceException(OrderServiceException ex) {
        log.warn("Order service exception: code={}, msg={}", ex.getErrorCode().getCode(), ex.getMessage());
        ErrorResponse body = new ErrorResponse(
                ex.getErrorCode().getCode(), ex.getMessage(), LocalDateTime.now().format(FORMATTER));
        return ResponseEntity.status(ex.getErrorCode().getHttpStatus()).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unexpected error", ex);
        ErrorResponse body = new ErrorResponse(
                OrderErrorCode.INTERNAL_ERROR.getCode(),
                OrderErrorCode.INTERNAL_ERROR.getMessage(),
                LocalDateTime.now().format(FORMATTER));
        return ResponseEntity.status(OrderErrorCode.INTERNAL_ERROR.getHttpStatus()).body(body);
    }

    public record ErrorResponse(String code, String message, String timestamp) {}
}
