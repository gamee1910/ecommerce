package io.game.service.orders.exception;

import com.gamee1910.error.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum OrderErrorCode implements ErrorCode {
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "ORD_001", "Order not found"),
    ORDER_ACCESS_DENIED(HttpStatus.FORBIDDEN, "ORD_002", "Access to order denied"),
    ORDER_CANNOT_BE_CANCELLED(HttpStatus.CONFLICT, "ORD_003", "Order cannot be cancelled in current status"),
    INSUFFICIENT_STOCK(HttpStatus.CONFLICT, "ORD_004", "Insufficient stock for one or more items"),
    PRODUCT_SERVICE_UNAVAILABLE(
            HttpStatus.SERVICE_UNAVAILABLE,
            "ORD_005",
            "Product service is currently unavailable, please try again later"),
    PRODUCT_UNAVAILABLE(HttpStatus.UNPROCESSABLE_ENTITY, "ORD_006", "One or more products are unavailable"),
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "CMN_001", "Validation failed"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "CMN_999", "Internal server error");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
