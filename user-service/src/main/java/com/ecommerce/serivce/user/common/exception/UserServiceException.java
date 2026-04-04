package com.ecommerce.serivce.user.common.exception;

import lombok.Getter;

@Getter
public class UserServiceException extends RuntimeException {

  private final UserServiceErrorCode errorCode;

  public UserServiceException(UserServiceErrorCode errorCode) {
    super(errorCode.getMessage());
    this.errorCode = errorCode;
  }
}
