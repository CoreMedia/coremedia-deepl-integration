package com.coremedia.labs.translation.deepl.workflow;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Properties to be used in {@code DeepL} settings document.
 */
@DefaultAnnotation(NonNull.class)
public class ConfigProperties {

  /**
   * Root node for DeepL Settings in the Struct.
   */
  public static final String KEY_DEEPL_ROOT = "deepl";

  /**
   * Base URL for DeepL API, may be overridden for testing purposes.
   * By default, the correct DeepL API (Free or Pro) is automatically selected.
   */
  public static final String KEY_SERVER_URL = "url";

  /**
   * API Key.
   */
  public static final String KEY_API_KEY = "apiKey";

  /**
   * Maximum number of failed HTTP requests to retry, the default is 5.
   * Note: only failures due to transient conditions are retried e.g. timeouts or temporary server overload.
   */
  public static final String KEY_MAX_RETRIES = "maxRetries";

  /**
   * Connection timeout for each HTTP request.
   */
  public static final String KEY_TIMEOUT = "timeout";

  /**
   * Proxy to use for all HTTP requests to DeepL.
   */
  public static final String KEY_PROXY = "proxy";

  /**
   * Additional HTTP headers to attach to all requests.
   */
  public static final String KEY_HEADERS = "headers";

  private ConfigProperties() {
  }

}
