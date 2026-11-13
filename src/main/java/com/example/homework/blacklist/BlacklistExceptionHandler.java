package com.example.homework.blacklist;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class BlacklistExceptionHandler {

  @ExceptionHandler(AlreadyBlacklistedException.class)
  public ProblemDetail handleAlreadyBlacklisted(AlreadyBlacklistedException e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
  }

  @ExceptionHandler(BlacklistEntryNotFoundException.class)
  public ProblemDetail handleNotFound(BlacklistEntryNotFoundException e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
  }
}
