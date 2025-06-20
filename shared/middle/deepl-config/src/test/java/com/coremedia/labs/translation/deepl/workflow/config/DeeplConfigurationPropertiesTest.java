package com.coremedia.labs.translation.deepl.workflow.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {
        DeeplConfigurationPropertiesTest.class
}, properties = {
        "deepl.api-key=apikey42",
        "deepl.pass-as-is-target-locales=en-US,en-GB",
        "deepl.proxy-url=http://proxy.example.com:8080",
        "deepl.client-options.timeout=42s",
        "deepl.client-options.max-retries=42",
        "deepl.text-translation-options.glossary-id=glossary42",
        "deepl.text-translation-options.model-type=quality_optimized",
        "deepl.text-translation-options.outline-detection=false",
        "deepl.text-translation-options.ignore-tags=tag1,tag2",
})
@Import(DeeplConfigurationConfiguration.class)
@EnableConfigurationProperties(DeeplConfigurationProperties.class)
class DeeplConfigurationPropertiesTest {

  @Autowired
  private DeeplConfigurationProperties config;

  @Test
  void testPropertySourceValues() {
    assertNotNull(config);
    assertEquals("apikey42", config.getApiKey());
    assertEquals(List.of("en-US", "en-GB"), config.getPassAsIsTargetLocales());
    assertEquals("http://proxy.example.com:8080", config.getProxyUrl());
    assertNotNull(config.getClientOptions().getProxy());
    assertEquals(Duration.ofSeconds(42), config.getClientOptions().getTimeout());
    assertEquals(42, config.getClientOptions().getMaxRetries());
    assertEquals("glossary42", config.getTextTranslationOptions().getGlossaryId());
    assertEquals("quality_optimized", config.getTextTranslationOptions().getModelType());
    assertFalse(config.getTextTranslationOptions().isOutlineDetection());
    assertEquals(List.of("tag1", "tag2"), config.getTextTranslationOptions().getIgnoreTags());
  }
}
