package com.coremedia.labs.translation.deepl.workflow.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "deepl")
public class DeeplConfigurationProperties extends DeeplConfiguration {

  public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
  public static final List<String> DEFAULT_PASS_AS_IS_TARGET_LOCALES = List.of("en-US", "en-GB", "pt-PT", "pt-BR", "zh-HANS", "zh-HANT");
  public static final Map<String, String> DEFAULT_FALLBACK_LOCALES_MAP = Map.of("en", "en-GB", "pt", "pt-PT");

  public DeeplConfigurationProperties() {
    super(DEFAULT_TIMEOUT, DEFAULT_PASS_AS_IS_TARGET_LOCALES, DEFAULT_FALLBACK_LOCALES_MAP);
  }

}
