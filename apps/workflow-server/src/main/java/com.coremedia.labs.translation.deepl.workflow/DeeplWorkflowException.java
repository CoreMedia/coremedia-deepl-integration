package com.coremedia.labs.translation.deepl.workflow;

import com.coremedia.cap.common.CapException;

import java.io.Serial;

import static java.util.Arrays.stream;

class DeeplWorkflowException extends CapException {
  @Serial
  private static final long serialVersionUID = 7674173301901498318L;

  DeeplWorkflowException(String errorCode, String message, Object... parameters) {
    this(errorCode, message, null, parameters);
  }

  DeeplWorkflowException(String errorCode, String message, Throwable cause, Object... parameters) {
    super(
      "deepl",
      errorCode,
      errorCode,
      message,
      stream(parameters).map(o -> o == null ? null : String.valueOf(o)).toArray(String[]::new),
      cause);
  }

}
