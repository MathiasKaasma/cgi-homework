package com.example.homework.notification;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import tools.jackson.databind.exc.InvalidFormatException;

@RestControllerAdvice
public class NotificationExceptionHandler {

  @ExceptionHandler(InvalidRecipientException.class)
  public ProblemDetail handleInvalidRecipient(InvalidRecipientException e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
  }

  @ExceptionHandler(RecipientBlacklistedException.class)
  public ProblemDetail handleBlacklisted(RecipientBlacklistedException e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, e.getMessage());
  }

  @ExceptionHandler(NotificationNotFoundException.class)
  public ProblemDetail handleNotFound(NotificationNotFoundException e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
  }

  @ExceptionHandler(NotificationNotUpdatableException.class)
  public ProblemDetail handleNotUpdatable(NotificationNotUpdatableException e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
  }

  @ExceptionHandler(RateLimitExceededException.class)
  public ProblemDetail handleRateLimited(RateLimitExceededException e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, e.getMessage());
  }

  @ExceptionHandler(CircuitOpenException.class)
  public ProblemDetail handleCircuitOpen(CircuitOpenException e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
  }

  @ExceptionHandler(ChannelDeliveryException.class)
  public ProblemDetail handleChannelDelivery(ChannelDeliveryException e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, e.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ProblemDetail handleValidation(MethodArgumentNotValidException e) {
    String detail = e.getBindingResult().getFieldErrors().stream()
        .map(error -> error.getField() + " " + error.getDefaultMessage())
        .collect(Collectors.joining(", "));
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ProblemDetail handleUnreadableBody(HttpMessageNotReadableException e) {
    if (e.getCause() instanceof InvalidFormatException invalid && invalid.getTargetType().isEnum()) {
      String field = invalid.getPath().getLast().getPropertyName();
      String allowed = Arrays.toString(invalid.getTargetType().getEnumConstants());
      return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, field + " must be one of " + allowed);
    }
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request body is not valid JSON");
  }
}
