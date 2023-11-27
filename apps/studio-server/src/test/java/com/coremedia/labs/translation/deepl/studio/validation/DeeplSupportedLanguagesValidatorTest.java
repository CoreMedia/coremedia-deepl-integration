package com.coremedia.labs.translation.deepl.studio.validation;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeeplSupportedLanguagesValidatorTest {

  private DeeplSupportedLanguagesValidator testling;

  @BeforeEach
  void setUp() {
    testling = new DeeplSupportedLanguagesValidator();
  }

  @Test
  void testIsValidLocale() {
    assertTrue(testling.isValidLocale(Locale.GERMANY, List.of(Locale.US, Locale.UK, Locale.GERMAN, Locale.GERMANY)));
  }

  @Test
  void testIsValidLocaleList() {
    List<Locale> localesToCheck = List.of(Locale.GERMANY, Locale.ITALY, Locale.FRANCE);
    List<Locale> validLocales = List.of(Locale.FRENCH, Locale.ITALIAN, Locale.GERMAN);
    assertTrue(testling.isValidLocaleList(localesToCheck, validLocales));
    assertFalse(testling.isValidLocaleList(List.of(Locale.UK, Locale.GERMANY), validLocales));
  }


}
