package io.game.service.orders.common.exception;

import lombok.Getter;

@Getter
public class OrderServiceException extends RuntimeException {

    private final OrderErrorCode errorCode;

    public OrderServiceException(OrderErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public OrderServiceException(OrderErrorCode errorCode, String detail) {
        super(detail);
        this.errorCode = errorCode;
    }
}
