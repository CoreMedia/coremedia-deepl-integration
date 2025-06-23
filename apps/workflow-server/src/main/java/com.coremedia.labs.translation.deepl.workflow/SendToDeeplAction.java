package com.coremedia.labs.translation.deepl.workflow;

import com.coremedia.cap.content.Content;
import com.coremedia.cap.content.ContentObject;
import com.coremedia.cap.multisite.ContentObjectSiteAspect;
import com.coremedia.cap.multisite.Site;
import com.coremedia.cap.translate.xliff.XliffExportOptions;
import com.coremedia.cap.translate.xliff.XliffExporter;
import com.coremedia.cap.translate.xliff.XliffImporter;
import com.coremedia.cap.workflow.Process;
import com.coremedia.cap.workflow.Task;
import com.coremedia.labs.translation.deepl.workflow.config.DeeplConfiguration;
import com.coremedia.labs.translation.deepl.workflow.config.DeeplConfigurationService;
import com.coremedia.translate.item.ContentToTranslateItemTransformer;
import com.coremedia.translate.item.TranslateItem;
import com.coremedia.translate.workflow.AsRobotUser;
import com.coremedia.translate.xliff.core.jaxb.Xliff;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import static com.coremedia.translate.item.TransformStrategy.ITEM_PER_TARGET;
import static java.lang.invoke.MethodHandles.lookup;

public class SendToDeeplAction extends DeeplAction {

  private static final Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

  public SendToDeeplAction() {
    super();
  }

  // --- LongAction interface ----------------------------------------------------------------------

  @Override
  public Object extractParameters(Task task) {
    Process process = task.getContainingProcess();
    List<Content> derivedContents = process.getLinks(derivedContentsVariable);
    List<ContentObject> masterContentObjects = process.getLinksAndVersions(masterContentObjectsVariable);
    return new Parameters(derivedContents, masterContentObjects);
  }

  @Override
  protected Object doExecute(Object params) throws Exception {
    Parameters parameters = (Parameters) params;

    // get Spring beans
    ApplicationContext springContext = getSpringContext();
    XliffExporter exporter = springContext.getBean("capXliffExporter", XliffExporter.class);
    XliffImporter importer = springContext.getBean("xliffImporter", XliffImporter.class);
    DeeplTranslationService translationService = springContext.getBean(DeeplTranslationService.class);
    ContentToTranslateItemTransformer transformer = springContext.getBean(ContentToTranslateItemTransformer.class);
    DeeplConfigurationService deeplConfigurationService = springContext.getBean(DeeplConfigurationService.class);

    if (parameters.derivedContents.isEmpty()) {
      return null;
    }

    // determine config and initialize translation service
    Site masterSite = getMasterSite(parameters.masterContentObjects);
    DeeplConfiguration config = deeplConfigurationService.getDeeplConfigurationForSite(masterSite);
    translationService.initialize(config);

    Map<Locale, List<TranslateItem>> translationItemsByLocale = transformer.transform(parameters.masterContentObjects,
                    parameters.derivedContents,
                    SendToDeeplAction::preferSiteLocale,
                    ITEM_PER_TARGET)
            .collect(Collectors.groupingBy(TranslateItem::getSingleTargetLocale));

    for (List<TranslateItem> localeListEntry : translationItemsByLocale.values()) {
      Xliff xliff = exporter.exportXliff(localeListEntry, XliffExportOptions.xliffExportOptions().option(XliffExportOptions.TargetOption.TARGET_SOURCE).build());
      translationService.translateXliff(xliff);
      try (AsRobotUser asRobotUser =  getAsRobotUser()) {
        asRobotUser.call(() -> importer.importXliff(xliff));
      }
    }
    return parameters.derivedContents;
  }

  AsRobotUser getAsRobotUser() {
    return new AsRobotUser(getConnection(), getSpringContext(), getCapSessionPool());
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

}
