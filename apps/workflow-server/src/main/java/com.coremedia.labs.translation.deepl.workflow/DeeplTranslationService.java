package com.coremedia.labs.translation.deepl.workflow;

import com.coremedia.cap.struct.Struct;
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
import com.deepl.api.DeepLException;
import com.deepl.api.TextResult;
import com.deepl.api.Translator;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xml.sax.InputSource;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

public class DeeplTranslationService {

  public static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>";
  public static final String SOURCE_PREFIX = "<source xmlns=\"urn:oasis:names:tc:xliff:document:1.2\">";
  public static final String SOURCE_SUFFIX = "</source>";
  private Translator translator;


  private static final Logger LOG = getLogger(lookup().lookupClass());


  public DeeplTranslationService() {
  }

  public void setTranslator(Translator translator) {
    this.translator = translator;
  }

  /**
   * Translate a single string
   *
   * @param toTranslate    to translate
   * @param sourceLanguage the language to translate
   * @param targetLanguage the language to translate to
   * @return a string if the translation succeed
   */
  public Optional<String> translate(String toTranslate, String sourceLanguage, String targetLanguage) {
    try {
      LOG.debug("Translating from {} to {}: {}", sourceLanguage, targetLanguage, toTranslate);
      TextResult textResult = translator.translateText(toTranslate, sourceLanguage, targetLanguage);
      return Optional.ofNullable(textResult.getText());
    } catch (DeepLException|InterruptedException e) {
      LOG.error("Unable to translate.", e);
    }

    // Unable to translate -> return input text
    return Optional.of(toTranslate);
  }

  /**
   * Translate a string that is a xliff
   *
   * @param xliff the xliff as string
   * @return a string if the translation succeed
   */

  public Optional<String> translateXliff(String xliff) {
    Xliff doc = parseXliff(xliff);
    translateXliff(doc);
    try {
      return Optional.of(convertToString(doc));
    } catch (Exception e) {
      LOG.warn("Ignoring exception.", e);
    }
    return Optional.empty();
  }

  /**
   * Translate a xliff
   *
   * @param xliff the xliff
   */
  public void translateXliff(Xliff xliff) {
    for (Object o : xliff.getAnyAndFile()) {
      if (o instanceof File) {
        File file = (File) o;
        String sourceLanguage = Locale.forLanguageTag(file.getSourceLanguage()).getLanguage();
        String targetLanguage = Locale.forLanguageTag(file.getTargetLanguage()).getLanguage();
        for (Object groupOrTransUnitOrBinUnit : file.getBody().getGroupOrTransUnitOrBinUnit()) {
          if (groupOrTransUnitOrBinUnit instanceof Group) {
            handleGroup((Group) groupOrTransUnitOrBinUnit, sourceLanguage, targetLanguage);
          } else {
            LOG.info("Not sure how to handle " + groupOrTransUnitOrBinUnit);
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
        LOG.info("Not sure how to handle " + groupOrTransUnitOrBinUnit);
      }
    }
  }

  private void handeTransUnit(TransUnit transUnit, String sourceLanguage, String targetLanguage) {
    transUnit.setApproved(AttrTypeYesNo.YES);
    Source source = transUnit.getSource();
    Target target = transUnit.getTarget();
    target.getContent().clear();
    Optional<String> sourceAsString = itemAsString(source, sourceLanguage, targetLanguage);
    if (sourceAsString.isPresent()) {
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
      LOG.error("Cannot marshal item " + source, e);
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
      return translate(key, sourceLanguage, targetLanguage);
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
      LOG.error("Cannot marshal item " + source, e);
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
