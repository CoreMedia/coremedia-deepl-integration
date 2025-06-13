package com.coremedia.labs.translation.deepl.workflow;


import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;

import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.time.Duration;
import java.util.Map;

import static java.lang.invoke.MethodHandles.lookup;

public class DeeplSettings {

  private static final Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

  /**
   * Root node for DeepL Settings in the Struct.
   */
  public static final String KEY_DEEPL_ROOT = "deepl";

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

  private String apiKey;
  private int maxRetries;
  private Duration timeout;
  private Proxy proxy;

  private DeeplSettings() {
  }

  public static DeeplSettings fromValues(Map<String, Object> values) {
    DeeplSettings result = new DeeplSettings();

    String apiKey = (String) values.get(KEY_API_KEY);

    int maxRetries = 5;
    Object maxRetriesValue = values.get(KEY_MAX_RETRIES);
    if (maxRetriesValue instanceof String) {
      maxRetries = Integer.parseInt((String) maxRetriesValue);
    } else if (maxRetriesValue instanceof Integer) {
      maxRetries = (int) maxRetriesValue;
    }

    Duration timeoutSeconds = Duration.ofSeconds(30L);
    Object timeoutValue = values.get(KEY_TIMEOUT);
    if (timeoutValue instanceof String) {
      timeoutSeconds = Duration.ofSeconds(Long.parseLong((String) timeoutValue));
    } else if (timeoutValue instanceof Long) {
      timeoutSeconds = Duration.ofSeconds((Long) timeoutValue);
    }

    String proxy = (String) values.get(KEY_PROXY);

    result.setApiKey(apiKey);
    result.setMaxRetries(maxRetries);
    result.setTimeout(timeoutSeconds);

    if (StringUtils.isNotBlank(proxy)) {
      try {
        URL proxyUrl = new URL(proxy);
        result.setProxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyUrl.getHost(), proxyUrl.getPort())));
      } catch (MalformedURLException e) {
        LOG.error("Cannot configure proxy.", e);
      }
    }

    return result;
  }

  public static DeeplSettings merge(DeeplSettings base, DeeplSettings overrides) {
    DeeplSettings result = new DeeplSettings();
    BeanUtils.copyProperties(base, result);
    BeanUtils.copyProperties(overrides, result);
    return result;
  }

  public String getApiKey() {
    return apiKey;
  }

  public void setApiKey(String apiKey) {
    this.apiKey = apiKey;
  }

  public int getMaxRetries() {
    return maxRetries;
  }

  public void setMaxRetries(int maxRetries) {
    this.maxRetries = maxRetries;
  }

  public Duration getTimeout() {
    return timeout;
  }

  public void setTimeout(Duration timeout) {
    this.timeout = timeout;
  }

  public Proxy getProxy() {
    return proxy;
  }

  public void setProxy(Proxy proxy) {
    this.proxy = proxy;
  }

}
