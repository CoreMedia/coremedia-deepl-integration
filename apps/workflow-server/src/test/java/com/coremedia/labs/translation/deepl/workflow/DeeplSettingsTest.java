package com.coremedia.labs.translation.deepl.workflow;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class DeeplSettingsTest {

  @Test
  public void testSettingsFromValues() {
    String apiBaseUrl = "https://api.deepl.com/";
    String apiKey = "se3cr3t";
    int maxRetries = 42;
    long timeout = 30L;
    String proxyHost = "my.proxy.net";
    String proxyPort = "8080";
    String headers = "X-Test=foo";

    Map<String, Object> values = Map.of(
            "url", apiBaseUrl,
            "apiKey", apiKey,
            "maxRetries", maxRetries,
            "timeout", timeout,
            "proxy", "https://" + proxyHost + ":" + proxyPort,
            "headers", headers
    );

    DeeplSettings settings = DeeplSettings.fromValues(values);
    assertEquals(apiBaseUrl, settings.getApiBaseUrl());
    assertEquals(apiKey, settings.getApiKey());
    assertEquals(maxRetries, settings.getMaxRetries());
    assertEquals(Duration.ofSeconds(timeout), settings.getTimeout());
    String expectedProxy = "HTTP @ " + proxyHost + ":" + proxyPort;
    String actualProxy = settings.getProxy().toString();
    assertEquals(expectedProxy, actualProxy);
    assertEquals(headers, settings.getHeaders());
  }

  @Test
  public void testMergeSetting() {

    Map<String, Object> values1 = Map.of(
            "url", "https://api1.deepl.com",
            "apiKey", "key1",
            "maxRetries", 5,
            "timeout", 30L
    );

    Map<String, Object> values2 = Map.of(
            "url", "https://api2.deepl.com",
            "apiKey", "key2"
    );

    DeeplSettings settings1 = DeeplSettings.fromValues(values1);
    DeeplSettings settings2 = DeeplSettings.fromValues(values2);

    DeeplSettings settings = DeeplSettings.merge(settings1, settings2);
    assertEquals("https://api2.deepl.com", settings.getApiBaseUrl());
    assertEquals("key2", settings.getApiKey());
    assertEquals(5, settings.getMaxRetries());
    assertEquals(Duration.ofSeconds(30L), settings.getTimeout());
  }

}
