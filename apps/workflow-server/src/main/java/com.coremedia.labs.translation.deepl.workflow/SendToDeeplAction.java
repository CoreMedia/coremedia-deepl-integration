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

import java.io.Serial;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.coremedia.translate.item.TransformStrategy.ITEM_PER_TARGET;
import static java.lang.invoke.MethodHandles.lookup;

public class SendToDeeplAction extends DeeplAction<SendToDeeplAction.Parameters, SendToDeeplAction.Result> {
  private static final Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

  @Serial
  private static final long serialVersionUID = -8558382288631077034L;

  public SendToDeeplAction() {
    super(true);
  }

  // --- DeeplAction interface ----------------------------------------------------------------------

  @Override
  Parameters doExtractParameters(Task task) {
    Process process = task.getContainingProcess();
    List<Content> derivedContents = process.getLinks(derivedContentsVariable);
    List<ContentObject> masterContentObjects = process.getLinksAndVersions(masterContentObjectsVariable);
    return new Parameters(derivedContents, masterContentObjects);
  }

  @Override
  void doExecuteDeeplAction(Parameters parameters,
                            Consumer<? super Result> resultConsumer,
                            Map<String, List<Content>> issues) throws Exception {
    // get Spring beans
    ApplicationContext springContext = getSpringContext();
    XliffExporter exporter = springContext.getBean("capXliffExporter", XliffExporter.class);
    XliffImporter importer = springContext.getBean("xliffImporter", XliffImporter.class);
    DeeplTranslationService translationService = springContext.getBean(DeeplTranslationService.class);
    ContentToTranslateItemTransformer transformer = springContext.getBean(ContentToTranslateItemTransformer.class);
    DeeplConfigurationService deeplConfigurationService = springContext.getBean(DeeplConfigurationService.class);
    if (parameters.derivedContents.isEmpty()) {
      return;
    }
    Result result = new Result();
    resultConsumer.accept(result);
    // determine config and initialize translation service
    Site masterSite = getMasterSite(parameters.masterContentObjects);
    DeeplConfiguration config = deeplConfigurationService.getDeeplConfigurationForSite(masterSite);
    translationService.initialize(config);
    Map<Locale, List<TranslateItem>> translationItemsByLocale = transformer.transform(parameters.masterContentObjects,
                    parameters.derivedContents,
                    SendToDeeplAction::preferSiteLocale,
                    ITEM_PER_TARGET)
            .collect(Collectors.groupingBy(TranslateItem::getSingleTargetLocale));
    // translate content
    for (List<TranslateItem> localeListEntry : translationItemsByLocale.values()) {
      Xliff xliff = exporter.exportXliff(localeListEntry, XliffExportOptions.xliffExportOptions().option(XliffExportOptions.TargetOption.TARGET_SOURCE).build());
      translationService.translateXliff(xliff, issues);
      try (AsRobotUser asRobotUser = getAsRobotUser()) {
        asRobotUser.call(() -> importer.importXliff(xliff));
      }
      importer.importXliff(xliff);
    }
  }

  AsRobotUser getAsRobotUser() {
    return new AsRobotUser(getConnection(), getSpringContext(), getCapSessionPool());
  }

  @Override
  Void doStoreResult(Task task, Result result) {
    return null;
  }

  private static Locale preferSiteLocale(ContentObjectSiteAspect aspect) {
    Site site = aspect.getSite();
    if (site == null) {
      return aspect.getLocale();
    }
    return site.getLocale();
  }

  record Parameters(Collection<Content> derivedContents, Collection<ContentObject> masterContentObjects) {
  }

  static final class Result {
  }
}
