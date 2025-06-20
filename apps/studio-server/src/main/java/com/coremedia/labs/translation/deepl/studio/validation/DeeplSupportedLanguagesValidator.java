package com.coremedia.labs.translation.deepl.studio.validation;

import com.coremedia.cap.content.ContentObject;
import com.coremedia.cap.multisite.ContentObjectSiteAspect;
import com.coremedia.cap.multisite.Site;
import com.coremedia.cap.multisite.SitesService;
import com.coremedia.labs.translation.deepl.workflow.config.DeeplConfiguration;
import com.coremedia.labs.translation.deepl.workflow.config.DeeplConfigurationService;
import com.coremedia.rest.cap.workflow.validation.WorkflowValidator;
import com.coremedia.rest.cap.workflow.validation.model.WorkflowValidationParameterModel;
import com.coremedia.rest.validation.Issues;
import com.coremedia.rest.validation.Severity;
import com.deepl.api.DeepLClient;
import com.deepl.api.DeepLException;
import com.deepl.api.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static java.lang.invoke.MethodHandles.lookup;

public class DeeplSupportedLanguagesValidator implements WorkflowValidator {
  private static final Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

  private final DeeplConfigurationService deeplConfigurationService;
  private final SitesService sitesService;

  public DeeplSupportedLanguagesValidator(DeeplConfigurationService deeplConfigurationService, SitesService sitesService) {
    this.deeplConfigurationService = deeplConfigurationService;
    this.sitesService = sitesService;
  }

  public static List<Locale> getSupportedSourceLocales(DeepLClient deeplClient) throws DeepLException, InterruptedException {
    List<String> supportedStrings = deeplClient.getSourceLanguages().stream().map(Language::getCode).toList();
    return supportedStrings.stream().map(Locale::forLanguageTag).collect(Collectors.toList());
  }

  public static List<Locale> getSupportedTargetLocales(DeepLClient deeplClient) throws DeepLException, InterruptedException {
    List<String> supportedStrings = deeplClient.getTargetLanguages().stream().map(Language::getCode).toList();
    return supportedStrings.stream().map(Locale::forLanguageTag).collect(Collectors.toList());
  }

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

    // get DeeplConfiguration
    Site mastersite = getMasterSite(workflowValidationParameterModel.getChangeSet());
    DeeplConfiguration config = deeplConfigurationService.getDeeplConfigurationForSite(mastersite);
    // if we don't have an apiKey, log a warning and skip validation
    String apiKey = config.getApiKey();
    if(apiKey == null || apiKey.isEmpty()) {
      LOG.warn("No API key configured for DeepL. Skipping language validation.");
      return;
    }
    DeepLClient deeplClient = new DeepLClient(apiKey, config.getClientOptions());
    // Validate source languages
    try {
      if (!sourceLocales.isEmpty()) {
        List<Locale> supportedSourceLocales = getSupportedSourceLocales(deeplClient);
        if(!isValidLocaleList(sourceLocales, supportedSourceLocales)) {
          issues.addIssue(Severity.ERROR, null, "unsupportedSourceLocales", getFirstInvalidLocale(sourceLocales, supportedSourceLocales));
        }
      }
    } catch (DeepLException | InterruptedException e) {
      throw new RuntimeException(e);
    }
    // Validate target languages
    try {
      if (!targetLocales.isEmpty()) {
        List<Locale> supportedTargetLocales = getSupportedTargetLocales(deeplClient);
        if (!isValidLocaleList(targetLocales, supportedTargetLocales)) {
          issues.addIssue(Severity.WARN, null, "unsupportedTargetLocales", getFirstInvalidLocale(targetLocales, supportedTargetLocales));
        }
      }
    } catch (DeepLException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Check if the given list of locales is valid by testing against the provided list of valid locales.
   *
   * @param localesToCheck
   * @param validLocales
   * @return
   */
  public static boolean isValidLocaleList(List<Locale> localesToCheck, List<Locale> validLocales) {
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
  public static boolean isValidLocale(Locale localeToCheck, List<Locale> validLocales) {
    Optional<Locale> match = validLocales.stream()
            .filter(l -> l.getLanguage().contains(localeToCheck.getLanguage()))
            .limit(1)
            .findFirst();
    return match.isPresent();
  }

  public static Optional<Locale> getFirstInvalidLocale(List<Locale> localesToCheck, List<Locale> validLocales) {
    for (Locale locale : localesToCheck) {
      if (!isValidLocale(locale, validLocales)) {
        return Optional.of(locale);
      }
    }
    return Optional.empty();
  }

  protected Site getMasterSite(Collection<? extends ContentObject> masterContents) {
    return masterContents.stream()
            .map(sitesService::getSiteAspect)
            .map(ContentObjectSiteAspect::getSite)
            .filter(Objects::nonNull)
            .findAny()
            .orElseThrow(() -> new IllegalStateException("No master site found"));
  }
}
