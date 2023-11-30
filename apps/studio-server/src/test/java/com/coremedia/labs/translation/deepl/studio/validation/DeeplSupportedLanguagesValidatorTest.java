package com.coremedia.labs.translation.deepl.studio.validation;


import com.coremedia.cap.multisite.SitesService;
import com.deepl.api.DeepLException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class DeeplSupportedLanguagesValidatorTest {

  private DeeplSupportedLanguagesValidator testling;

  @BeforeEach
  void setUp() {
    testling = new DeeplSupportedLanguagesValidator(sitesService);
  }

  @Test
  void testIsValidLocale() {
    assertTrue(testling.isValidLocale(Locale.GERMANY, List.of(Locale.US, Locale.UK, Locale.GERMAN, Locale.GERMANY)));
  }

  @Test
  void testIsValidLocaleList() throws DeepLException, InterruptedException {
    List<Locale> localesToCheck = List.of(Locale.GERMANY, Locale.ITALY, Locale.FRANCE);
    List<Locale> validLocales = List.of(Locale.FRENCH, Locale.ITALIAN, Locale.GERMAN);
    assertTrue(testling.isValidLocaleList(localesToCheck, testling.getSupportedSourceLocales()));
    assertFalse(testling.isValidLocaleList(List.of(Locale.UK, Locale.GERMANY), validLocales));
  }




  @Test
  void testGetSupportedSourceLocales() throws DeepLException, InterruptedException {
    List<Locale> SUPPORTED_SOURCE_LOCALES = Stream.of("BG", "CS", "DA", "DE", "EL", "EN", "ES", "ET", "FI", "FR", "HU", "ID", "IT", "JA", "KO", "LT", "LV", "NB", "NL", "PL", "PT", "RO", "RU", "SK", "SL", "SV", "TR", "UK", "ZH").map(Locale::forLanguageTag).collect(Collectors.toList());
    assertEquals(SUPPORTED_SOURCE_LOCALES, testling.getSupportedSourceLocales());
  }
  @Test
  void testGetSupportedTargetLocales() throws DeepLException, InterruptedException {
    List<Locale> SUPPORTED_TARGET_LOCALES = Stream.of("BG", "CS", "DA", "DE", "EL", "EN", "EN-GB", "EN-US", "ES", "ET", "FI", "FR", "HU", "ID", "IT", "JA", "KO", "LT", "LV", "NB", "NL", "PL", "PT", "PT-BR", "PT-PT", "RO", "RU", "SK", "SL", "SV", "TR", "UK", "ZH").map(Locale::forLanguageTag).collect(Collectors.toList());
    assertEquals(SUPPORTED_TARGET_LOCALES, testling.getSupportedTargetLocales());
  }

  @Test
  void testGetFirstInvalidLocale() {
    List<Locale> localesToCheck = List.of(Locale.GERMANY,Locale.JAPAN, Locale.ITALY, Locale.FRANCE, Locale.CHINESE);
    List<Locale> validLocales = List.of(Locale.FRENCH, Locale.ITALIAN, Locale.GERMAN);
    assertEquals(Optional.of(Locale.JAPAN), testling.getFirstInvalidLocale(localesToCheck,validLocales));

  }
  @Test
  void testValidLocales() {
    List<Locale> localesToCheck = List.of(Locale.GERMANY,Locale.ITALY, Locale.FRANCE);
    List<Locale> validLocales = List.of(Locale.FRENCH, Locale.ITALIAN, Locale.GERMAN);
    assertNull(testling.getFirstInvalidLocale(localesToCheck, validLocales));

  }
}
