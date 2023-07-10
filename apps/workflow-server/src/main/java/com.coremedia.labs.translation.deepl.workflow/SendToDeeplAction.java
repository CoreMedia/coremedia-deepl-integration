package com.coremedia.labs.translation.deepl.workflow;

import com.coremedia.cap.content.Content;
import com.coremedia.cap.content.ContentObject;
import com.coremedia.cap.multisite.ContentObjectSiteAspect;
import com.coremedia.cap.multisite.Site;
import com.coremedia.cap.multisite.SitesService;
import com.coremedia.cap.struct.Struct;
import com.coremedia.cap.translate.xliff.XliffExportOptions;
import com.coremedia.cap.translate.xliff.XliffExporter;
import com.coremedia.cap.translate.xliff.XliffImporter;
import com.coremedia.cap.util.StructUtil;
import com.coremedia.cap.workflow.Process;
import com.coremedia.cap.workflow.Task;
import com.coremedia.translate.item.ContentToTranslateItemTransformer;
import com.coremedia.translate.item.TranslateItem;
import com.coremedia.translate.xliff.core.jaxb.Xliff;
import com.coremedia.workflow.common.util.SpringAwareLongAction;
import com.deepl.api.Translator;
import com.deepl.api.TranslatorOptions;
import com.google.common.annotations.VisibleForTesting;
import edu.umd.cs.findbugs.annotations.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.coremedia.translate.item.TransformStrategy.ITEM_PER_TARGET;
import static java.lang.invoke.MethodHandles.lookup;

public class SendToDeeplAction extends SpringAwareLongAction {

  private static final Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

  private static final String LOCAL_SETTINGS = "localSettings";
  private static final String LINKED_SETTINGS = "linkedSettings";
  private static final String CMSETTINGS_SETTINGS = "settings";

  private XliffExporter exporter;
  private XliffImporter importer;
  private DeeplTranslationService translationService;

  private String derivedContentsVariable;
  private String masterContentObjectsVariable;

  public SendToDeeplAction() {
    super(true);
  }

  // --- LongAction interface ----------------------------------------------------------------------

  @Override
  public Object extractParameters(Task task) {
    Process process = task.getContainingProcess();
    setExporter(getSpringContext().getBean("capXliffExporter", XliffExporter.class));
    setTranslationService(getSpringContext().getBean("deeplTranslationService", DeeplTranslationService.class));
    XliffImporter xliffImporter = getSpringContext().getBean("xliffImporter", XliffImporter.class);
    setImporter(xliffImporter);

    List<Content> derivedContents = process.getLinks(derivedContentsVariable);
    List<ContentObject> masterContentObjects = process.getLinksAndVersions(masterContentObjectsVariable);

    return new Parameters(derivedContents, masterContentObjects);
  }

  @Override
  protected Object doExecute(Object params) throws Exception {
    Parameters parameters = (Parameters) params;

    if (parameters.derivedContents.isEmpty()) {
      return null;
    }

    //Site masterSite = getMasterSite(parameters.masterContentObjects);

    Map<Locale, List<TranslateItem>> translationItemsByLocale = getTranslationItemsByLocale(parameters.masterContentObjects, parameters.derivedContents, SendToDeeplAction::preferSiteLocale);

    for (List<TranslateItem> localeListEntry : translationItemsByLocale.values()) {
      Xliff xliff = exporter.exportXliff(localeListEntry, XliffExportOptions.xliffExportOptions().option(XliffExportOptions.TargetOption.TARGET_SOURCE).build());
      translationService.translateXliff(xliff, true);
      importer.importXliff(xliff);
    }
    return parameters.derivedContents;
  }

  private Map<Locale, List<TranslateItem>> getTranslationItemsByLocale(
          Collection<ContentObject> masterContentObjects,
          Collection<Content> derivedContents,
          Function<ContentObjectSiteAspect, Locale> localeMapper) {

    ContentToTranslateItemTransformer transformer = getSpringContext().getBean(ContentToTranslateItemTransformer.class);

    return transformer.transform(masterContentObjects, derivedContents, localeMapper, ITEM_PER_TARGET).collect(Collectors.groupingBy(TranslateItem::getSingleTargetLocale));
  }

  private static Locale preferSiteLocale(ContentObjectSiteAspect aspect) {
    Site site = aspect.getSite();
    if (site == null) {
      return aspect.getLocale();
    }
    return site.getLocale();
  }

  private static final class Parameters {
    public final Collection<Content> derivedContents;
    public final Collection<ContentObject> masterContentObjects;

    public Parameters(final Collection<Content> derivedContents,
                      final Collection<ContentObject> masterContentObjects) {
      this.derivedContents = derivedContents;
      this.masterContentObjects = masterContentObjects;
    }
  }

  /**
   * Return the name of the process variable that stores the list of contents
   * for which a translation should be generated.
   *
   * @return the name of the process variable
   */
  public String getDerivedContentsVariable() {
    return derivedContentsVariable;
  }

  /**
   * Return the name of the process variable that stores the list of contents
   * for which a translation should be generated.
   *
   * @param derivedContentsVariable the name of the process variable
   */
  public void setDerivedContentsVariable(String derivedContentsVariable) {
    this.derivedContentsVariable = derivedContentsVariable;
  }

  /**
   * Return the name of the process variable containing the source contents objects.
   *
   * @return the name of the process variable
   */
  public String getMasterContentObjectsVariable() {
    return masterContentObjectsVariable;
  }

  /**
   * Set the name of the process variable containing the source contents objects.
   *
   * @param masterContentObjectsVariable the name of the process variable
   */
  public void setMasterContentObjectsVariable(String masterContentObjectsVariable) {
    this.masterContentObjectsVariable = masterContentObjectsVariable;
  }

  public void setExporter(XliffExporter exporter) {
    this.exporter = exporter;
  }

  public void setTranslationService(DeeplTranslationService translationService) {
    this.translationService = translationService;
  }

  public void setImporter(XliffImporter importer) {
    this.importer = importer;
  }


  // --- Internal -------------------------------------------------------------

  private SitesService getSitesService() {
    ApplicationContext springContext = getSpringContext();
    return springContext.getBean(SitesService.class);
  }

  private Site getMasterSite(Collection<? extends ContentObject> masterContents) {
    SitesService sitesService = getSitesService();
    return masterContents.stream()
            .map(sitesService::getSiteAspect)
            .map(ContentObjectSiteAspect::getSite)
            .filter(Objects::nonNull)
            .findAny()
            .orElseThrow(() -> new IllegalStateException("No master site found"));
  }

  @VisibleForTesting
  Map<String, Object> getSettings(Site site) {
    Content siteIndicator = site.getSiteIndicator();

    @SuppressWarnings("unchecked")
    Map<String, Object> defaultSettings = new HashMap<String, Object>(getSpringContext().getBean("gccConfigurationProperties", Map.class));

    Map<String, Object> siteIndicatorSettings = getSettings(siteIndicator);
    if (!siteIndicatorSettings.isEmpty()) {
      defaultSettings.putAll(siteIndicatorSettings);
      return Collections.unmodifiableMap(defaultSettings);
    }

    Content siteRootDocument = site.getSiteRootDocument();
    Map<String, Object> rootDocumentSettings = getSettings(siteRootDocument);
    if (!rootDocumentSettings.isEmpty()) {
      defaultSettings.putAll(rootDocumentSettings);
      return Collections.unmodifiableMap(defaultSettings);
    }

    return defaultSettings;
  }

  private static Map<String, Object> getSettings(Content content) {
    Struct localSettings = getStruct(content, LOCAL_SETTINGS);
    Struct struct = StructUtil.mergeStructList(
            localSettings,
            content.getLinks(LINKED_SETTINGS)
                    .stream()
                    .map(link -> getStruct(link, CMSETTINGS_SETTINGS))
                    .collect(Collectors.toList())
    );
    if (struct != null) {
      Object value = struct.get(ConfigProperties.KEY_DEEPL_ROOT);
      if (value instanceof Struct) {
        return ((Struct) value).toNestedMaps();
      }
    }

    return Collections.emptyMap();
  }


  @Nullable
  private static Struct getStruct(Content content, String name) {
    if (content != null && content.isInProduction()) {
      return content.getStruct(name);
    }
    return null;
  }

  private void initSession(Site site) {
    Map<String, Object> deeplSettings = getSettings(site);
    String apiKey = String.valueOf(deeplSettings.get(ConfigProperties.KEY_API_KEY));

    if (StringUtils.isBlank(apiKey)) {
      throw new IllegalStateException("No DeepL API key configured.");
    }

    int maxRetries = (Integer) deeplSettings.get(ConfigProperties.KEY_MAX_RETRIES);
    Duration timeoutSeconds = Duration.ofSeconds((Long) deeplSettings.get(ConfigProperties.KEY_TIMEOUT));
    String proxyUrlSetting = String.valueOf(deeplSettings.get(ConfigProperties.KEY_PROXY));

    TranslatorOptions options = new TranslatorOptions();
    options.setMaxRetries(maxRetries);
    options.setTimeout(timeoutSeconds);

    if (StringUtils.isNotBlank(proxyUrlSetting)) {
      try {
        URL proxyUrl = new URL(proxyUrlSetting);
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyUrl.getHost(), proxyUrl.getPort()));
        options.setProxy(proxy);
      } catch (MalformedURLException e) {
        LOG.error("Cannot configure proxy.", e);
      }
    }

    Translator translator = new Translator(apiKey, options);
    translationService.setTranslator(translator);
  }

}