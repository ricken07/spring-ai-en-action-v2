package com.rickenbazolo.refund.api.reliability;

public class AiExecutionException extends RuntimeException {

  public AiExecutionException(String message, Throwable cause) {
    super(message, cause);
  }
}
