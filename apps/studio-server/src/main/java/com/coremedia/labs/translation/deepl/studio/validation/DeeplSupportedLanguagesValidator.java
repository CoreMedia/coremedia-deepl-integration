package com.coremedia.labs.translation.deepl.studio.validation;

import com.coremedia.cap.content.Content;
import com.coremedia.cap.content.ContentObject;
import com.coremedia.cap.multisite.ContentObjectSiteAspect;
import com.coremedia.cap.multisite.Site;
import com.coremedia.cap.multisite.SitesService;
import com.coremedia.cap.struct.Struct;
import com.coremedia.cap.util.StructUtil;
import com.coremedia.rest.cap.workflow.validation.WorkflowValidator;
import com.coremedia.rest.cap.workflow.validation.model.WorkflowValidationParameterModel;
import com.coremedia.rest.validation.Issues;
import com.coremedia.rest.validation.Severity;
import com.deepl.api.*;
import edu.umd.cs.findbugs.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class DeeplSupportedLanguagesValidator implements WorkflowValidator {
  private static final String LOCAL_SETTINGS = "localSettings";
  private static final String LINKED_SETTINGS = "linkedSettings";
  private static final String CMSETTINGS_SETTINGS = "settings";
  public static final String KEY_DEEPL_ROOT = "deepl";
  public static final String KEY_API_KEY = "apiKey";
  private String apiKey;

  //  private static final List<Locale> SUPPORTED_SOURCE_LOCALES = Stream.of("BG", "CS", "DA", "DE", "EL", "EN", "ES", "ET", "FI", "FR", "HU", "ID", "IT", "JA", "KO", "LT", "LV", "NB", "NL", "PL", "PT", "RO", "RU", "SK", "SL", "SV", "TR", "UK", "ZH").map(Locale::forLanguageTag).collect(Collectors.toList());
  //  private static final List<Locale> SUPPORTED_TARGET_LOCALES = Stream.of("BG", "CS", "DA", "DE", "EL", "EN", "EN-GB", "EN-US", "ES", "ET", "FI", "FR", "HU", "ID", "IT", "JA", "KO", "LT", "LV", "NB", "NL", "PL", "PT", "PT-BR", "PT-PT", "RO", "RU", "SK", "SL", "SV", "TR", "UK", "ZH").map(Locale::forLanguageTag).collect(Collectors.toList());
  private Translator translator;

  private SitesService sitesService;


  public DeeplSupportedLanguagesValidator(SitesService sitesService) {
    this.sitesService = sitesService;
  }

  public List<Locale> getSupportedSourceLocales() throws DeepLException, InterruptedException {
    translator = new Translator(apiKey);
    List<String> supportedStrings = translator.getSourceLanguages().stream().map(Language::getCode).collect(Collectors.toList());
    return supportedStrings.stream().map(Locale::new).collect(Collectors.toList());
  }

  public List<Locale> getSupportedTargetLocales() throws DeepLException, InterruptedException {
    translator = new Translator(apiKey);
    List<String> supportedStrings = translator.getTargetLanguages().stream().map(Language::getCode).collect(Collectors.toList());
    return supportedStrings.stream().map(Locale::new).collect(Collectors.toList());
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

    // Get ApiKey
    Site mastersite = getMasterSite(workflowValidationParameterModel.getChangeSet());
    apiKey = getApiKey(mastersite.getSiteRootDocument());

    // Validate source languages
    try {
      if (!sourceLocales.isEmpty() && !isValidLocaleList(sourceLocales, getSupportedSourceLocales())) {
        issues.addIssue(Severity.ERROR, null, "unsupportedSourceLocales", getFirstInvalidLocale(sourceLocales, getSupportedSourceLocales()));
      }
    } catch (DeepLException | InterruptedException e) {
      throw new RuntimeException(e);
    }
    // Validate target languages
    try {
      if (!targetLocales.isEmpty() && !isValidLocaleList(targetLocales, getSupportedTargetLocales())) {
        issues.addIssue(Severity.WARN, null, "unsupportedTargetLocales", getFirstInvalidLocale(targetLocales, getSupportedTargetLocales()));
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
            .filter(l -> l.getLanguage().contains(localeToCheck.getLanguage()))
            .limit(1)
            .findFirst();
    return match.isPresent();
  }

  public Optional<Locale> getFirstInvalidLocale(List<Locale> localesToCheck, List<Locale> validLocales) {
    for (Locale locale : localesToCheck) {
      if (!isValidLocale(locale, validLocales)) {
        return Optional.of(locale);
      }
    }
    return Optional.empty();
  }

  public String getApiKey(Content content) {
    Struct localSettings = getStruct(content, LOCAL_SETTINGS);
    Struct struct = StructUtil.mergeStructList(
            localSettings,
            content.getLinks(LINKED_SETTINGS)
                    .stream()
                    .map(link -> getStruct(link, CMSETTINGS_SETTINGS))
                    .collect(Collectors.toList())
    );

    Map<String, Object> structSettings = new HashMap<>();

    if (struct != null) {
      Object value = struct.get(KEY_DEEPL_ROOT);
      if (value instanceof Struct) {
        structSettings = ((Struct) value).toNestedMaps();
      }
    }
    return structSettings.get(KEY_API_KEY).toString();
  }

  @Nullable
  private static Struct getStruct(Content content, String name) {
    if (content != null && content.isInProduction()) {
      return content.getStruct(name);
    }
    return null;
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
