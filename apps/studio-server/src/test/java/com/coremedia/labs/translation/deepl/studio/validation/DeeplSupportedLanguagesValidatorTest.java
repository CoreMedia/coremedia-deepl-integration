package com.coremedia.labs.translation.deepl.studio.validation;


import com.coremedia.labs.translation.deepl.workflow.config.DeeplConfigurationProperties;
import com.deepl.api.DeepLClient;
import com.deepl.api.DeepLException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {
        DeeplSupportedLanguagesValidatorTest.class
})
@EnableConfigurationProperties(DeeplConfigurationProperties.class)
class DeeplSupportedLanguagesValidatorTest {
  private static final String API_KEY_ENV_VAR = "DEEPL_API_KEY";

  @Autowired
  private DeeplConfigurationProperties deeplConfig;

  @Test
  void testIsValidLocale() {
    assertTrue(DeeplSupportedLanguagesValidator.isValidLocale(Locale.GERMANY, List.of(Locale.US, Locale.UK, Locale.GERMAN, Locale.GERMANY)));
  }

  @Test
  @EnabledIfEnvironmentVariable(named = API_KEY_ENV_VAR, matches = ".*")
  void testIsValidLocaleList() throws DeepLException, InterruptedException {
    DeepLClient deepLClient = new DeepLClient(deeplConfig.getApiKey(), deeplConfig.getClientOptions());
    List<Locale> localesToCheck = List.of(Locale.GERMANY, Locale.ITALY, Locale.FRANCE);
    List<Locale> validLocales = List.of(Locale.FRENCH, Locale.ITALIAN, Locale.GERMAN);
    assertTrue(DeeplSupportedLanguagesValidator.isValidLocaleList(localesToCheck, DeeplSupportedLanguagesValidator.getSupportedSourceLocales(deepLClient)));
    assertFalse(DeeplSupportedLanguagesValidator.isValidLocaleList(List.of(Locale.UK, Locale.GERMANY), validLocales));
  }

  @Test
  @EnabledIfEnvironmentVariable(named = API_KEY_ENV_VAR, matches = ".*")
  void testGetSupportedSourceLocales() throws DeepLException, InterruptedException {
    DeepLClient deepLClient = new DeepLClient(deeplConfig.getApiKey(), deeplConfig.getClientOptions());
    List<Locale> SUPPORTED_SOURCE_LOCALES = Stream.of("AR", "BG", "CS", "DA", "DE", "EL", "EN", "ES", "ET", "FI", "FR", "HU", "ID", "IT", "JA", "KO", "LT", "LV", "NB", "NL", "PL", "PT", "RO", "RU", "SK", "SL", "SV", "TR", "UK", "ZH").map(Locale::forLanguageTag).collect(Collectors.toList());
    assertEquals(SUPPORTED_SOURCE_LOCALES, DeeplSupportedLanguagesValidator.getSupportedSourceLocales(deepLClient));
  }

  @Test
  @EnabledIfEnvironmentVariable(named = API_KEY_ENV_VAR, matches = ".*")
  void testGetSupportedTargetLocales() throws DeepLException, InterruptedException {
    DeepLClient deepLClient = new DeepLClient(deeplConfig.getApiKey(), deeplConfig.getClientOptions());
    List<Locale> SUPPORTED_TARGET_LOCALES = Stream.of("AR", "BG", "CS", "DA", "DE", "EL", "EN-GB", "EN-US", "ES", "ET", "FI", "FR", "HU", "ID", "IT", "JA", "KO", "LT", "LV", "NB", "NL", "PL", "PT-BR", "PT-PT", "RO", "RU", "SK", "SL", "SV", "TR", "UK", "ZH", "ZH-HANS", "ZH-HANT").map(Locale::forLanguageTag).collect(Collectors.toList());
    assertEquals(SUPPORTED_TARGET_LOCALES, DeeplSupportedLanguagesValidator.getSupportedTargetLocales(deepLClient));
  }

  @Test
  void testGetFirstInvalidLocale() {
    List<Locale> localesToCheck = List.of(Locale.GERMANY, Locale.JAPAN, Locale.ITALY, Locale.FRANCE, Locale.CHINESE);
    List<Locale> validLocales = List.of(Locale.FRENCH, Locale.ITALIAN, Locale.GERMAN);
    assertEquals(Optional.of(Locale.JAPAN), DeeplSupportedLanguagesValidator.getFirstInvalidLocale(localesToCheck, validLocales));
  }

  @Test
  void testValidLocales() {
    List<Locale> localesToCheck = List.of(Locale.GERMANY, Locale.ITALY, Locale.FRANCE);
    List<Locale> validLocales = List.of(Locale.FRENCH, Locale.ITALIAN, Locale.GERMAN);
    assertEquals(DeeplSupportedLanguagesValidator.getFirstInvalidLocale(localesToCheck, validLocales), Optional.empty());
  }
}
