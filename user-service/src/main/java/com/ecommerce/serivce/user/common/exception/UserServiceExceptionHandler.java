package com.ecommerce.serivce.user.common.exception;

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
@Slf4j(topic = "UserServiceException")
public class UserServiceExceptionHandler {
  @ExceptionHandler(UserServiceException.class)
  public ResponseEntity<UserServiceErrorResponse> handleUserServiceException(
      UserServiceException ex) {
    UserServiceErrorCode errorCode = ex.getErrorCode();
    return ResponseEntity.status(errorCode.getHttpStatus())
        .body(UserServiceErrorResponse.of(errorCode));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<UserServiceErrorResponse> handlValidation(
      MethodArgumentNotValidException ex) {
    Map<String, String> errors =
        ex.getBindingResult().getFieldErrors().stream()
            .collect(
                Collectors.toMap(
                    FieldError::getField,
                    fieldError ->
                        Objects.requireNonNullElse(fieldError.getDefaultMessage(), "Invalid value"),
                    (existingValue, newValue) -> existingValue));
    return ResponseEntity.status(UserServiceErrorCode.VALIDATION_FAILED.getHttpStatus())
        .body(UserServiceErrorResponse.of(UserServiceErrorCode.VALIDATION_FAILED, errors));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<UserServiceErrorResponse> handleGeneric(Exception ex) {
    log.error("Unhandled exception", ex);
    return ResponseEntity.status(UserServiceErrorCode.INTERNAL_ERROR.getHttpStatus())
        .body(UserServiceErrorResponse.of(UserServiceErrorCode.INTERNAL_ERROR));
  }
}
