package com.coremedia.labs.translation.deepl.studio.validation;

import com.coremedia.cap.multisite.Site;
import com.coremedia.rest.cap.workflow.validation.WorkflowValidator;
import com.coremedia.rest.cap.workflow.validation.model.WorkflowValidationParameterModel;
import com.coremedia.rest.validation.Issues;
import com.coremedia.rest.validation.Severity;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DeeplSupportedLanguagesValidator implements WorkflowValidator {

  // TODO: Fetch list of supported locales from Deepl API
  private static final List<Locale> SUPPORTED_SOURCE_LOCALES = Stream.of("BG", "CS", "DA", "DE", "EL", "EN", "ES", "ET", "FI", "FR", "HU", "ID", "IT", "JA", "KO", "LT", "LV", "NB", "NL", "PL", "PT", "RO", "RU", "SK", "SL", "SV", "TR", "UK", "ZH").map(Locale::forLanguageTag).collect(Collectors.toList());
  private static final List<Locale> SUPPORTED_TARGET_LOCALES = Stream.of("BG", "CS", "DA", "DE", "EL", "EN", "EN-GB", "EN-US", "ES", "ET", "FI", "FR", "HU", "ID", "IT", "JA", "KO", "LT", "LV", "NB", "NL", "PL", "PT", "PT-BR", "PT-PT", "RO", "RU", "SK", "SL", "SV", "TR", "UK", "ZH").map(Locale::forLanguageTag).collect(Collectors.toList());


  @Override
  public void addIssuesIfInvalid(Issues issues, WorkflowValidationParameterModel workflowValidationParameterModel, Runnable runnable) {
    // Check if source or target languages contains unsupported languages

    // Get source locales from workflow parameter model
    List<Locale> sourceLocales = workflowValidationParameterModel.getChangeSet().stream()
            .map(c -> c.getString("locale"))
            .filter(Objects::nonNull)
            .map(Locale::forLanguageTag)
            .distinct()
            .collect(Collectors.toList());

    // Get target locales from workflow parameter model
    List<Locale> targetLocales = workflowValidationParameterModel.getAssignedSites().stream()
            .map(Site::getLocale)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());

    // Validate source languages
    if (!sourceLocales.isEmpty() && !isValidLocaleList(sourceLocales, SUPPORTED_SOURCE_LOCALES)) {
      issues.addIssue(Severity.ERROR, null, "unsupportedSourceLocales");
    }

    // Validate target languages
    if (!targetLocales.isEmpty() && !isValidLocaleList(targetLocales, SUPPORTED_TARGET_LOCALES)) {
      issues.addIssue(Severity.ERROR, null, "unsupportedTargetLocales");
    }

  }

  /**
   * Check if the given list of locales is valid by testing against the provided list of valid locales.
   *
   * @param localesToCheck
   * @param validLocales
   * @return
   */
  public boolean isValidLocaleList(List<Locale> localesToCheck, List<Locale> validLocales) {
    return localesToCheck.stream()
            .filter(l -> !isValidLocale(l, validLocales))
            .limit(1)
            .findFirst()
            .isEmpty();
  }

  /**
   * Check if the given locale is valid by testing against the provided list of valid locales.
   *
   * @param localeToCheck
   * @param validLocales
   * @return
   */
  public boolean isValidLocale(Locale localeToCheck, List<Locale> validLocales) {
    Optional<Locale> match = validLocales.stream()
            .filter(l -> l.getLanguage().equals(localeToCheck.getLanguage()))
            .limit(1)
            .findFirst();
    return match.isPresent();
  }

}
