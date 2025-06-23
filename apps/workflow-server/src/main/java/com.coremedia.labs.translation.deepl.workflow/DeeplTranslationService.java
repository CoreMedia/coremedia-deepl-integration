package com.coremedia.labs.translation.deepl.workflow;

import com.coremedia.cap.common.IdHelper;
import com.coremedia.cap.content.Content;
import com.coremedia.cap.content.ContentRepository;
import com.coremedia.labs.translation.deepl.workflow.config.DeeplConfiguration;
import com.coremedia.translate.xliff.core.jaxb.*;
import com.deepl.api.DeepLClient;
import com.deepl.api.DeepLException;
import com.deepl.api.TextResult;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

public class DeeplTranslationService {
  private static final Logger LOG = getLogger(lookup().lookupClass());

  public static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>";
  public static final String SOURCE_PREFIX = "<source xmlns=\"urn:oasis:names:tc:xliff:document:1.2\">";
  public static final String SOURCE_SUFFIX = "</source>";

  private final DeeplConfiguration config;
  private final ContentRepository contentRepository;
  private DeepLClient deepLClient;

  public DeeplTranslationService(DeeplConfiguration config, ContentRepository contentRepository) {
    this.config = config;
    this.contentRepository = contentRepository;
  }

  public void initialize(DeeplConfiguration config) {
    this.deepLClient = new DeepLClient(config.getApiKey(), config.getClientOptions());
  }

  /**
   * Translate a single string
   *
   * @param toTranslate    to translate
   * @param sourceLanguage the language to translate
   * @param targetLanguage the language to translate to
   * @return a string if the translation succeed
   */
  public Optional<String> translate(String toTranslate, String sourceLanguage, String targetLanguage) throws DeepLException, InterruptedException {
    LOG.debug("Translating from {} to {}: {}", sourceLanguage, targetLanguage, toTranslate);
    TextResult textResult = deepLClient.translateText(toTranslate, sourceLanguage, targetLanguage, config.getTextTranslationOptions());
    return Optional.ofNullable(textResult.getText());
  }

  /**
   * Translate a xliff
   *
   * @param xliff  the xliff
   * @param issues the issues map to fill with any issues that occur during translation
   */
  public void translateXliff(Xliff xliff, Map<String, List<Content>> issues) throws Exception {
    for (Object o : xliff.getAnyAndFile()) {
      if (o instanceof File file) {
        try {
          handleFile(file);
        } catch (JAXBException jbe) {
          String original = file.getOriginal();
          LOG.warn("Failed to process {}.", original, jbe);
          addTranslationIssue(issues, original);
        }
      }
    }
  }

  private void addTranslationIssue(Map<String, List<Content>> issues, String versionId) {
    try {
      int contentId = IdHelper.parseContentIdFromVersionId(versionId);
      Content sourceContent = contentRepository.getContent(String.valueOf(contentId));
      List<Content> sourceContents = issues.computeIfAbsent(DeeplWorkflowErrorCodes.ITEM_TRANSLATION_FAILURE, k -> new ArrayList<>());
      sourceContents.add(sourceContent);
    } catch (Exception e) {
      LOG.warn("Failed to add translation issue for {}.", versionId, e);
    }
  }

  /**
   * Return the targetLanguage to use for DeepL.
   * See <a href="https://developers.deepl.com/docs/getting-started/supported-languages">DeepL Supported Languages</a>.
   *
   * @param targetLocale Target locale
   * @return targetLanguage to use for DeepL
   */
  private String getDeepLTargetLanguage(Locale targetLocale, DeeplConfiguration config) {
    String languageTag = targetLocale.toLanguageTag();
    // strip the country unless country variant is explicitly supported/required by DeepL
    if (config.getPassAsIsTargetLocales().contains(languageTag)) {
      return languageTag;
    }
    return targetLocale.getLanguage();
  }

  private void handleFile(File file) throws Exception {
    // DeepL (currently) only supports the language part of a locale for sourceLanguage
    String sourceLanguage = Locale.forLanguageTag(file.getSourceLanguage()).getLanguage();
    // determine target language to use for DeepL
    String targetLanguage = getDeepLTargetLanguage(Locale.forLanguageTag(file.getTargetLanguage()), config);
    for (Object groupOrTransUnitOrBinUnit : file.getBody().getGroupOrTransUnitOrBinUnit()) {
      if (groupOrTransUnitOrBinUnit instanceof Group group) {
        handleGroup(group, sourceLanguage, targetLanguage);
      } else {
        LOG.info("Not sure how to handle " + groupOrTransUnitOrBinUnit);
      }
    }
  }

  private void handleGroup(Group group, String sourceLanguage, String targetLanguage) throws Exception {
    for (Object groupOrTransUnitOrBinUnit : group.getGroupOrTransUnitOrBinUnit()) {
      if (groupOrTransUnitOrBinUnit instanceof TransUnit transUnit) {
        handeTransUnit(transUnit, sourceLanguage, targetLanguage);
      } else if (groupOrTransUnitOrBinUnit instanceof Group subGroup) {
        handleGroup(subGroup, sourceLanguage, targetLanguage);
      } else {
        LOG.info("Not sure how to handle " + groupOrTransUnitOrBinUnit);
      }
    }
  }

  private void handeTransUnit(TransUnit transUnit, String sourceLanguage, String targetLanguage) throws Exception {
    transUnit.setApproved(AttrTypeYesNo.YES);
    Source source = transUnit.getSource();
    Target target = transUnit.getTarget();
    target.getContent().clear();
    Optional<String> sourceAsString = itemAsString(source, sourceLanguage, targetLanguage);
    if (sourceAsString.isPresent()) {
      Optional<Source> item = stringToItem(sourceAsString.get());
      if (item.isPresent()) {
        handleContent(item.get().getContent(), target, sourceLanguage, targetLanguage);
      }
      target.setState("translated");
    } else {
      handleContent(source.getContent(), target, sourceLanguage, targetLanguage);
    }
  }

  private Optional<String> itemAsString(Object source, String sourceLanguage, String targetLanguage) throws JAXBException, DeepLException, InterruptedException {
    StringWriter writer = new StringWriter();
    JAXBContext.newInstance(Xliff.class).createMarshaller().marshal(source, writer);
    String result = writer.toString();
    if (result.startsWith(XML_HEADER)) {
      result = result.substring(XML_HEADER.length());
    }
    if (result.startsWith(SOURCE_PREFIX) && result.endsWith(SOURCE_SUFFIX)) {
      result = result.substring(SOURCE_PREFIX.length(), result.length() - SOURCE_SUFFIX.length());
    }

    String key = result.replace("\n", "").replaceAll("\\s+", " ").trim();
    if (StringUtils.isNotBlank(key)) {
      return translate(key, sourceLanguage, targetLanguage);
    }

    return Optional.empty();
  }

  private Optional<Source> stringToItem(String source) throws JAXBException {
    StringReader reader = new StringReader(XML_HEADER + SOURCE_PREFIX + source + SOURCE_SUFFIX);
    Object element = JAXBContext.newInstance(Xliff.class).createUnmarshaller().unmarshal(reader);
    if (element instanceof Source) {
      return Optional.of((Source) element);
    } else {
      return Optional.empty();
    }
  }

  private void handleContent(List<Object> contents, Target target, String sourceLanguage, String targetLanguage) throws Exception {
    for (Object sourceEntry : contents) {
      if (sourceEntry instanceof String) {
        target.getContent().add(sourceEntry);
      } else if (sourceEntry instanceof G) {
        G targetG = handleG((G) sourceEntry, sourceLanguage, targetLanguage);
        target.getContent().add(targetG);
      } else if (sourceEntry instanceof X) {
        X targetX = handleX((X) sourceEntry);
        target.getContent().add(targetX);
      } else if (sourceEntry instanceof Ph) {
        Ph targetPh = handePh((Ph) sourceEntry);
        target.getContent().add(targetPh);
      } else {
        LOG.info("Not sure how to handle " + sourceEntry);
      }
    }
  }

  private Ph handePh(Ph source) {
    Ph target = new Ph();
    target.getContent().addAll(source.getContent());
    target.setCtype(source.getCtype());
    target.setCrc(source.getCrc());
    target.setAssoc(source.getAssoc());
    target.setId(source.getId());
    target.setXid(source.getXid());
    target.setEquivText(source.getEquivText());
    target.getOtherAttributes().putAll(source.getOtherAttributes());
    return target;
  }

  private X handleX(X source) {
    X target = new X();
    target.setCtype(source.getCtype());
    target.setClone(source.getClone());
    target.setId(source.getId());
    target.setXid(source.getXid());
    target.setEquivText(source.getEquivText());
    target.getOtherAttributes().putAll(source.getOtherAttributes());
    return target;
  }

  private G handleG(G source, String sourceLanguage, String targetLanguage) {
    G target = new G();
    target.setCtype(source.getCtype());
    target.setClone(source.getClone());
    target.setId(source.getId());
    target.setXid(source.getXid());
    target.setEquivText(source.getEquivText());
    target.getOtherAttributes().putAll(source.getOtherAttributes());
    for (Object sourceEntry : source.getContent()) {
      if (sourceEntry instanceof String || sourceEntry instanceof G) {
        target.getContent().add(sourceEntry);
      } else {
        LOG.warn("Unable to handle: {}", sourceEntry);
      }
    }
    return target;
  }

}
