package com.coremedia.labs.translation.deepl.workflow.config;

import com.deepl.api.Formality;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.Proxy;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class DeeplConfigurationTest {
  private DeeplConfiguration config;

  @BeforeEach
  void setUp() {
    config = new DeeplConfiguration();
    config.setApiKey("apiKey");
    config.getClientOptions().setTimeout(Duration.ofSeconds(42));
    config.getClientOptions().setMaxRetries(42);
    config.setPassAsIsTargetLocales(Arrays.asList("EN-GB", "EN-US"));
    config.getTextTranslationOptions().setFormality(Formality.More);
    config.getTextTranslationOptions().setGlossaryId("glossaryId");
    config.getTextTranslationOptions().setSplittingTags(List.of("splitTag1", "splitTag2"));
    config.getTextTranslationOptions().setIgnoreTags(List.of("ignoreTag1", "ignoreTag2"));
  }

  @Test
  public void testProxy() {
    config.setProxyUrl("http://proxy.example.com:8080");
    Proxy proxy = config.getClientOptions().getProxy();
    assertNotNull(proxy);
  }

  @Test
  public void testFromDeeplConfiguration() {
    DeeplConfiguration copy = DeeplConfiguration.from(config);
    // check that all properties have been copied
    assertNotSame(copy, config);
    assertNotSame(copy.getPassAsIsTargetLocales(), config.getPassAsIsTargetLocales());
    assertNotSame(copy.getClientOptions(), config.getClientOptions());
    assertNotSame(copy.getTextTranslationOptions(), config.getTextTranslationOptions());
    assertEquals(config, copy);
  }

  @Test
  public void testFromMap() {
    config.setProxyUrl("http://proxy.example.com:8080");
    Map<String, Object> map = new HashMap<>();
    map.put("apiKey", "apiKey");
    map.put("proxyUrl", "http://proxy.example.com:8080");
    map.put("passAsIsTargetLocales", List.of("EN-GB", "EN-US"));
    map.put("clientOptions", Map.of("maxRetries", 42,
            "timeout", "42s"));
    map.put("textTranslationOptions", Map.of("formality", Formality.More.name(),
            "glossaryId", "glossaryId",
            "splittingTags", List.of("splitTag1", "splitTag2"),
            "ignoreTags", List.of("ignoreTag1", "ignoreTag2")));
    DeeplConfiguration configFromMap = DeeplConfiguration.from(map);
    assertEquals(config, configFromMap);
    assertNotNull(configFromMap.getClientOptions().getProxy());
  }

  @Test
  public void testMergeWithDeeplConfiguration() {
    DeeplConfiguration toMerge = DeeplConfiguration.from(config);
    toMerge.getClientOptions().setMaxRetries(4242);
    toMerge.getTextTranslationOptions().setGlossaryId("glossaryId2");
    toMerge.getTextTranslationOptions().setFormality(Formality.Less);
    toMerge.getTextTranslationOptions().setModelType("quality_optimized");
    toMerge.getTextTranslationOptions().setSplittingTags(List.of("splitTag1", "splitTag2", "splitTag3"));
    toMerge.getTextTranslationOptions().setIgnoreTags(null);
    DeeplConfiguration merged = DeeplConfiguration.merge(config, toMerge);
    assertEquals(4242, merged.getClientOptions().getMaxRetries());
    assertEquals("glossaryId2", merged.getTextTranslationOptions().getGlossaryId());
    assertEquals(Formality.Less, merged.getTextTranslationOptions().getFormality());
    assertEquals("quality_optimized", merged.getTextTranslationOptions().getModelType());
    assertEquals(List.of("splitTag1", "splitTag2", "splitTag3"), merged.getTextTranslationOptions().getSplittingTags());
    assertEquals(List.of("EN-GB", "EN-US"), merged.getPassAsIsTargetLocales());
    assertEquals(List.of("ignoreTag1", "ignoreTag2"), merged.getTextTranslationOptions().getIgnoreTags());
  }

  @Test
  public void testMergeWithMap() {
    config.getClientOptions().setSendPlatformInfo(false);
    config.getTextTranslationOptions().setPreserveFormatting(true);
    config.getTextTranslationOptions().setOutlineDetection(false);
    Map<String, Object> map = new HashMap<>();
    map.put("clientOptions", Map.of("maxRetries", 4242,
            "timeout", "4242s"));
    List<String> splittingTags = List.of("splitTag1", "splitTag2", "splitTag3");
    map.put("textTranslationOptions", Map.of("formality", Formality.More.name(),
            "glossaryId", "glossaryId2",
            "splittingTags", splittingTags));
    DeeplConfiguration merged = DeeplConfiguration.merge(config, map);
    assertEquals(config.getApiKey(), merged.getApiKey());
    assertEquals(4242, merged.getClientOptions().getMaxRetries());
    assertEquals(Duration.ofSeconds(4242), merged.getClientOptions().getTimeout());
    assertEquals("glossaryId2", merged.getTextTranslationOptions().getGlossaryId());
    assertEquals(splittingTags, merged.getTextTranslationOptions().getSplittingTags());
    assertEquals(List.of("ignoreTag1", "ignoreTag2"), merged.getTextTranslationOptions().getIgnoreTags());
    // check that default properties not in map are still intact
    assertEquals(config.getClientOptions().getSendPlatformInfo(), merged.getClientOptions().getSendPlatformInfo());
    assertEquals(config.getTextTranslationOptions().isPreserveFormatting(), merged.getTextTranslationOptions().isPreserveFormatting());
    assertEquals(config.getTextTranslationOptions().isOutlineDetection(), merged.getTextTranslationOptions().isOutlineDetection());
  }
}
