package com.ecommerce.service.product.common.exception;

import lombok.Getter;

@Getter
public class ProductServiceException extends RuntimeException {

    private final ProductServiceErrorCode errorCode;

    public ProductServiceException(ProductServiceErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
