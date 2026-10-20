package com.rickenbazolo.refund.application;

public final class ForbiddenOrderAccessException extends RuntimeException {
  public ForbiddenOrderAccessException() {
    super("Order access denied");
  }
}
