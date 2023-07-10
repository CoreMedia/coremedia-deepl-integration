package com.coremedia.labs.translation.deepl.workflow;

import com.coremedia.cap.content.Content;
import com.coremedia.cap.content.ContentRepository;
import com.coremedia.cap.struct.Struct;
import com.coremedia.cap.struct.StructService;
import com.coremedia.translate.xliff.core.jaxb.AttrTypeYesNo;
import com.coremedia.translate.xliff.core.jaxb.File;
import com.coremedia.translate.xliff.core.jaxb.G;
import com.coremedia.translate.xliff.core.jaxb.Group;
import com.coremedia.translate.xliff.core.jaxb.Ph;
import com.coremedia.translate.xliff.core.jaxb.Source;
import com.coremedia.translate.xliff.core.jaxb.Target;
import com.coremedia.translate.xliff.core.jaxb.TransUnit;
import com.coremedia.translate.xliff.core.jaxb.X;
import com.coremedia.translate.xliff.core.jaxb.Xliff;
import com.deepl.api.Translator;
import com.google.common.annotations.VisibleForTesting;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xml.sax.InputSource;

import javax.annotation.PostConstruct;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

public class DeeplTranslationService {
  private static final String DEFAULT_TRANSLATION_PROVIDERS = "/System/Translation Providers";
  private static final String DEFAULT_TRANSLATION_MEMORY = "/System/Translation Memory";
  private static final String TRANSLATIONS = "translations";
  private static final String TARGET_LOCALES = "targetLocales";
  public static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>";
  public static final String SOURCE_PREFIX = "<source xmlns=\"urn:oasis:names:tc:xliff:document:1.2\">";
  public static final String SOURCE_SUFFIX = "</source>";
  private final ContentRepository contentRepository;
  private Content translationMemoryContent;
  private Content translationProvidersContent;
  private Translator translator;
  private StructService structService;
  private String translationMemoryPath = DEFAULT_TRANSLATION_MEMORY;
  private String translationProvidersPath = DEFAULT_TRANSLATION_PROVIDERS;


  private static final Logger LOGGER = getLogger(lookup().lookupClass());


  public DeeplTranslationService(ContentRepository contentRepository) {
    this.contentRepository = contentRepository;
  }

  @VisibleForTesting
  void setTranslationMemoryPath(String translationMemoryPath) {
    this.translationMemoryPath = translationMemoryPath;
  }

  @VisibleForTesting
  void setTranslationProvidersPath(String translationProvidersPath) {
    this.translationProvidersPath = translationProvidersPath;
  }

  public void setTranslator(Translator translator) {
    this.translator = translator;
  }

  @PostConstruct
  void initialize() {
    this.structService = contentRepository.getConnection().getStructService();
    this.translationMemoryContent = this.contentRepository.getChild(translationMemoryPath);
    this.translationProvidersContent = this.contentRepository.getChild(translationProvidersPath);
  }

  /**
   * Translate a single string
   *
   * @param toTranslate    to translate
   * @param sourceLanguage the language to translate
   * @param targetLanguage the language to translate to
   * @param wait           do a fake initial wait?
   * @return a string if the translation succeed
   */
  public Optional<String> translate(String toTranslate, String sourceLanguage, String targetLanguage, boolean wait) {
    waitIfRequested(wait, targetLanguage);
    return translateAndUpdate(toTranslate, sourceLanguage, targetLanguage);
  }

  /**
   * Translate a string that is a xliff
   *
   * @param xliff the xliff as string
   * @param wait  do a fake initial wait?
   * @return a string if the translation succeed
   */

  public Optional<String> translateXliff(String xliff, boolean wait) {
    Xliff doc = parseXliff(xliff);
    translateXliff(doc, wait);
    try {
      return Optional.of(convertToString(doc));
    } catch (Exception e) {
      LOGGER.warn("Ignoring exception.", e);
    }
    return Optional.empty();
  }

  /**
   * Translate a xliff
   *
   * @param xliff the xliff
   * @param wait  do a fake initial wait?
   */

  public void translateXliff(Xliff xliff, Boolean wait) {
    for (Object o : xliff.getAnyAndFile()) {
      if (o instanceof File) {
        File file = (File) o;
        String sourceLanguage = Locale.forLanguageTag(file.getSourceLanguage()).getLanguage();
        String targetLanguage = Locale.forLanguageTag(file.getTargetLanguage()).getLanguage();
        waitIfRequested(wait, targetLanguage);
        for (Object groupOrTransUnitOrBinUnit : file.getBody().getGroupOrTransUnitOrBinUnit()) {
          if (groupOrTransUnitOrBinUnit instanceof Group) {
            handleGroup((Group) groupOrTransUnitOrBinUnit, sourceLanguage, targetLanguage);
          } else {
            LOGGER.info("Not sure how to handle " + groupOrTransUnitOrBinUnit);
          }
        }
      }
    }
  }

  private void handleGroup(Group group, String sourceLanguage, String targetLanguage) {
    for (Object groupOrTransUnitOrBinUnit : group.getGroupOrTransUnitOrBinUnit()) {
      if (groupOrTransUnitOrBinUnit instanceof TransUnit) {
        handeTransUnit((TransUnit) groupOrTransUnitOrBinUnit, sourceLanguage, targetLanguage);
      } else if (groupOrTransUnitOrBinUnit instanceof Group) {
        handleGroup((Group) groupOrTransUnitOrBinUnit, sourceLanguage, targetLanguage);
      } else {
        LOGGER.info("Not sure how to handle " + groupOrTransUnitOrBinUnit);
      }
    }
  }

  private void handeTransUnit(TransUnit transUnit, String sourceLanguage, String targetLanguage) {
    transUnit.setApproved(AttrTypeYesNo.YES);
    Source source = transUnit.getSource();
    Target target = transUnit.getTarget();
    target.getContent().clear();
    Optional<String> sourceAsString = itemAsString(source, sourceLanguage, targetLanguage);
    if(sourceAsString.isPresent()) {
      sourceAsString.flatMap(this::stringToItem).ifPresent(item -> handleContent(item.getContent(), target, sourceLanguage, targetLanguage));
      target.setState("translated");
    } else {
      handleContent(source.getContent(), target, sourceLanguage, targetLanguage);
    }
  }

  private Optional<String> itemAsString(Object source, String sourceLanguage, String targetLanguage) {
    StringWriter writer = new StringWriter();
    try {
      JAXBContext.newInstance(Xliff.class).createMarshaller().marshal(source, writer);
    } catch (JAXBException e) {
      LOGGER.error("Cannot marshal item " + source, e);
      return Optional.empty();
    }

    String result = writer.toString();
    if (result.startsWith(XML_HEADER)) {
      result = result.substring(XML_HEADER.length());
    }
    if (result.startsWith(SOURCE_PREFIX) && result.endsWith(SOURCE_SUFFIX)) {
      result = result.substring(SOURCE_PREFIX.length(), result.length() - SOURCE_SUFFIX.length());
    }

    String key = result.replace("\n", "").replaceAll("\\s+", " ").trim();
    if (StringUtils.isNotBlank(key)) {
      return translate(key, sourceLanguage, targetLanguage, false);
    }

    return Optional.empty();
  }

  private Optional<Source> stringToItem(String source) {
    StringReader reader = new StringReader(XML_HEADER + SOURCE_PREFIX + source + SOURCE_SUFFIX);
    try {
      Object element = JAXBContext.newInstance(Xliff.class).createUnmarshaller().unmarshal(reader);
      if (element instanceof Source) {
        return Optional.of((Source) element);
      }
    } catch (JAXBException e) {
      LOGGER.error("Cannot marshal item " + source, e);
    }
    return Optional.empty();
  }

  private void handleContent(List<Object> contents, Target target, String sourceLanguage, String targetLanguage) {
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
        LOGGER.info("Not sure how to handle " + sourceEntry);
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
      if (sourceEntry instanceof String) {
        target.getContent().add(sourceEntry);
      }
    }
    return target;
  }

  /**
   * Translates the given string from the provided source language to the provided target language
   * and updates the translation memory, if the translation does not exist yet.
   *
   * @param text text to be translated
   * @param sourceLanguage source language code
   * @param targetLanguage target language code
   * @return {@link Optional} with the translated string, or empty result.
   */
  private Optional<String> translateAndUpdate(String text, String sourceLanguage, String targetLanguage) {
    if (this.translationProvidersContent != null) {
      Struct targetLocales = this.translationProvidersContent.getStruct("settings").getStruct(TARGET_LOCALES);
      if (targetLocales != null && hasPropertyDescriptor(targetLocales, targetLanguage)) {
        Struct configurationMap = targetLocales.getStruct(targetLanguage);
        if (configurationMap != null) {
          boolean translationEnabled = false;
          if (hasPropertyDescriptor(configurationMap, "translationEnabled")) {
            translationEnabled = configurationMap.getBoolean("translationEnabled");
          }
          return updateTargetContent(text, getString(configurationMap, "translationService"), translationEnabled, sourceLanguage, targetLanguage);
        }
      } else {
        LOGGER.warn("No entries found for locale " + targetLanguage);
      }
    }
    return Optional.empty();
  }

  private void waitIfRequested(Boolean wait, String targetLanguage) {
    if (this.translationProvidersContent != null) {
      Struct targetLocales = this.translationProvidersContent.getStruct("settings").getStruct(TARGET_LOCALES);
      if (targetLocales != null && hasPropertyDescriptor(targetLocales, targetLanguage)) {
        Struct configurationMap = targetLocales.getStruct(targetLanguage);
        if (wait && configurationMap != null && hasPropertyDescriptor(configurationMap, "timeToWait")) {
          Integer timeToWait = configurationMap.getInteger("timeToWait");
          if (timeToWait != null) {
            try {
              Thread.sleep(timeToWait * 1000L);
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
          }
        }
      }
    }
  }


  private Optional<String> updateTargetContent(String sourceEntry, String translationService, boolean enabled, String sourceLanguage, String targetLanguage) {
    String key = sourceEntry.replace("\n", "").replaceAll("\\s+", " ").trim();
    if (StringUtils.isNotBlank(key) && this.translationMemoryContent != null) {
      Optional<String> alreadyTranslated = translationMemoryContent.getStruct("settings").getStructs(TRANSLATIONS).stream()
              .filter(struct -> findString(struct, sourceLanguage).isPresent() && findString(struct, targetLanguage).isPresent())
              .filter(struct -> Objects.equals(getString(struct, sourceLanguage), key))
              .map(struct -> getString(struct, targetLanguage))
              .findFirst();

      if (alreadyTranslated.isPresent()) {
        LOGGER.info("Found entry {} for key: {}", alreadyTranslated.get(), key);
        return alreadyTranslated;
      } else {
        LOGGER.info("Cannot find value for key: " + key);
        Optional<String> translation = translate(key, translationService, enabled, sourceLanguage, targetLanguage);
        translation.ifPresent(trans -> updateTranslationMemory(trans, key, sourceLanguage, targetLanguage));
        return translation;
      }
    }
    return Optional.empty();
  }

  private void updateTranslationMemory(String translation, String key, String sourceLanguage, String targetLanguage) {
    int i = 0;
    boolean foundEntry = false;
    for (Struct struct : translationMemoryContent.getStruct("settings").getStructs(TRANSLATIONS)) {
      String sourceLocale = getString(struct, sourceLanguage);
      if (sourceLocale != null) {
        if (sourceLocale.equals(key)) {
          Struct newStruct = struct.builder().declareString(targetLanguage, Integer.MAX_VALUE, translation).build();
          Struct updated = translationMemoryContent.getStruct("settings").builder().set(TRANSLATIONS, i, newStruct).build();
          updateTranslationMemoryStruct(updated);
          foundEntry = true;
        }
      }
      i++;
    }

    if (!foundEntry && (!sourceLanguage.equals(targetLanguage))) {
      Struct struct = structService.emptyStruct().builder().declareString(sourceLanguage, Integer.MAX_VALUE, key).declareString(targetLanguage, Integer.MAX_VALUE, translation).build();
      Struct updated = translationMemoryContent.getStruct("settings").builder().add(TRANSLATIONS, struct).build();
      updateTranslationMemoryStruct(updated);
    }
  }

  private void updateTranslationMemoryStruct(Struct updated) {
    try {
      if (translationMemoryContent.isCheckedIn()) {
        translationMemoryContent.checkOut();
      }
      translationMemoryContent.set("settings", updated);
      contentRepository.getConnection().flush();
    } catch (Exception e) {
      LOGGER.warn("Error updating translation memory", e);
    }
  }


  private Optional<String> translate(String key, String translationService, boolean enabled, String sourceLanguage, String targetLanguage) {
    // TODO: Implement
    return Optional.empty();
  }

  private static String convertToString(Xliff xliff) {
    try {
      StringWriter writer = new StringWriter();
      JAXBContext context;
      context = JAXBContext.newInstance("com.coremedia.translate.xliff.core.jaxb");
      Marshaller m = context.createMarshaller();
      m.marshal(xliff, writer);
      return writer.toString();
    } catch (JAXBException e) {
      throw new IllegalStateException("could not marshal group", e);
    }
  }

  private static Xliff parseXliff(String untranslatedXliff) {
    try {
      JAXBContext context;
      context = JAXBContext.newInstance("com.coremedia.translate.xliff.core.jaxb");
      Unmarshaller m = context.createUnmarshaller();
      InputSource src = new InputSource(new StringReader(untranslatedXliff));
      return (Xliff) m.unmarshal(src);
    } catch (JAXBException e) {
      throw new IllegalStateException("could not marshal group", e);
    }
  }

  @Nullable
  private static String getString(@NonNull Struct struct, @NonNull String key) {
    if (hasPropertyDescriptor(struct, key)) {
      return struct.getString(key);
    } else {
      return null;
    }
  }

  private static Optional<String> findString(@NonNull Struct struct, @NonNull String key) {
    return Optional.ofNullable(getString(struct, key));
  }

  private static boolean hasPropertyDescriptor(@NonNull Struct struct, @NonNull String key) {
    return struct.getType().getDescriptor(key) != null;
  }
}
