package com.coremedia.labs.translation.deepl.workflow.config;

import com.deepl.api.DeepLClientOptions;
import com.deepl.api.TextTranslationOptions;
import org.apache.commons.lang3.StringUtils;
import org.modelmapper.AbstractConverter;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.spi.MappingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.convert.DurationStyle;

import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.lang.invoke.MethodHandles.lookup;

public class DeeplConfiguration {
  private static final Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

  /**
   * API Key.
   */
  protected String apiKey;

  /**
   * Proxy URL.
   */
  protected String proxyUrl;

  /**
   * DeeplClientOptions.
   */
  protected DeepLClientOptions clientOptions;

  /**
   * TextTranslationOptions.
   */
  protected TextTranslationOptions textTranslationOptions;

  /**
   * List of target locales that should be passed as-is as DeepL target language.
   * See <a href="https://developers.deepl.com/docs/getting-started/supported-languages">Supported Languages.</a>.
   */
  protected List<String> passAsIsTargetLocales;

  /**
   * Map of fallback locales for languages when the requested target language is not supported by DeepL.
   * Key: language code, Value: fallback locale to use instead.
   */
  protected Map<String, String> fallbackLocalesForLanguages;

  protected List<Map<String, String>> glossaries;

  public DeeplConfiguration() {
    this.clientOptions = new DeepLClientOptions();
    this.textTranslationOptions = new TextTranslationOptions();
  }

  public DeeplConfiguration(Duration defaultTimeout,
                            List<String> defaultPassAsIsTargetLocales,
                            Map<String, String> defaultFallbackLocalesForLanguages) {
    DeepLClientOptions clientOptions = new DeepLClientOptions();
    clientOptions.setTimeout(defaultTimeout);
    this.clientOptions = clientOptions;
    this.textTranslationOptions = new TextTranslationOptions();
    this.passAsIsTargetLocales = defaultPassAsIsTargetLocales;
    this.fallbackLocalesForLanguages = defaultFallbackLocalesForLanguages;
  }

  public String getApiKey() {
    return apiKey;
  }

  public void setApiKey(String apiKey) {
    this.apiKey = apiKey;
  }

  public String getProxyUrl() {
    return proxyUrl;
  }

  public void setProxyUrl(String proxyUrl) {
    if (StringUtils.isNotBlank(proxyUrl)) {
      try {
        URL proxyU = new URL(proxyUrl);
        clientOptions.setProxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyU.getHost(), proxyU.getPort())));
        this.proxyUrl = proxyUrl;
      } catch (MalformedURLException e) {
        LOG.error("Cannot configure proxy.", e);
      }
    }
  }

  public DeepLClientOptions getClientOptions() {
    return clientOptions;
  }

  public void setClientOptions(DeepLClientOptions clientOptions) {
    this.clientOptions = clientOptions;
  }

  public TextTranslationOptions getTextTranslationOptions() {
    return textTranslationOptions;
  }

  public void setTextTranslationOptions(TextTranslationOptions textTranslationOptions) {
    this.textTranslationOptions = textTranslationOptions;
  }

  public List<String> getPassAsIsTargetLocales() {
    return passAsIsTargetLocales;
  }

  public void setPassAsIsTargetLocales(List<String> passAsIsTargetLocales) {
    this.passAsIsTargetLocales = Collections.unmodifiableList(passAsIsTargetLocales);
  }

  public Map<String, String> getFallbackLocalesForLanguages() {
    return fallbackLocalesForLanguages;
  }

  public void setFallbackLocalesForLanguages(Map<String, String> fallbackLocalesForLanguages) {
    this.fallbackLocalesForLanguages = fallbackLocalesForLanguages;
  }

  public List<Map<String, String>> getGlossaries() {
    return glossaries;
  }

  public void setGlossaries(List<Map<String, String>> glossaries) {
    this.glossaries = glossaries;
  }

  public static DeeplConfiguration from(DeeplConfiguration source) {
    DeeplConfiguration copy = new DeeplConfiguration();
    ModelMapper mapper = new ModelMapper();
    mapper.getConfiguration().setDeepCopyEnabled(true);
    mapper.map(source, copy);
    // manually copy properties that are not handled correctly by ModelMapper
    copy.getClientOptions().setTimeout(source.getClientOptions().getTimeout());
    copy.getTextTranslationOptions().setSplittingTags(source.getTextTranslationOptions().getSplittingTags());
    copy.getTextTranslationOptions().setIgnoreTags(source.getTextTranslationOptions().getIgnoreTags());
    copy.getTextTranslationOptions().setNonSplittingTags(source.getTextTranslationOptions().getNonSplittingTags());
    return copy;
  }

  public static DeeplConfiguration from(Map<String, Object> source) {
    DeeplConfiguration config = new DeeplConfiguration();
    ModelMapper mapper = new ModelMapper();
    mapper.addConverter(new AbstractConverter<String, Duration>() {
      @Override
      protected Duration convert(String source) {
        return DurationStyle.detectAndParse(source);
      }
    });
    mapper.map(source, config);
    return config;
  }

  public static DeeplConfiguration merge(DeeplConfiguration defaultConfig, DeeplConfiguration other) {
    DeeplConfiguration config = from(defaultConfig);
    ModelMapper mapper = new ModelMapper();
    mapper.getConfiguration().setDeepCopyEnabled(true);
    mapper.getConfiguration().setSkipNullEnabled(true);
    // special handling for certain nested properties that ModelMapper does not handle correctly
    Converter<Duration, Duration> durationConverter = MappingContext::getSource;
    mapper.createTypeMap(DeepLClientOptions.class, DeepLClientOptions.class)
            .addMappings(deepLClientOptionsMapper -> {
              deepLClientOptionsMapper.using(durationConverter)
                      .map(DeepLClientOptions::getTimeout, DeepLClientOptions::setTimeout);
            });
    Converter<Iterable<String>, Iterable<String>> stringIterableConverter = MappingContext::getSource;
    mapper.createTypeMap(TextTranslationOptions.class, TextTranslationOptions.class)
            .addMappings(textTranslationOptionsMapper -> {
              textTranslationOptionsMapper.using(stringIterableConverter)
                      .map(TextTranslationOptions::getNonSplittingTags, TextTranslationOptions::setNonSplittingTags);
              textTranslationOptionsMapper.using(stringIterableConverter)
                      .map(TextTranslationOptions::getSplittingTags, TextTranslationOptions::setSplittingTags);
              textTranslationOptionsMapper.using(stringIterableConverter)
                      .map(TextTranslationOptions::getIgnoreTags, TextTranslationOptions::setIgnoreTags);
            });
    mapper.map(other, config);
    return config;
  }

  public static DeeplConfiguration merge(DeeplConfiguration defaultConfig, Map<String, Object> otherMap) {
    DeeplConfiguration other = DeeplConfiguration.from(otherMap);
    DeeplConfiguration merged = merge(defaultConfig, other);
    // special handling for DeepLClientOptions not in map
    if (otherMap.get("clientOptions") instanceof Map clientOptionsMap) {
      if (!clientOptionsMap.containsKey("maxRetries")) {
        merged.getClientOptions().setMaxRetries(defaultConfig.getClientOptions().getMaxRetries());
      }
      if (!clientOptionsMap.containsKey("sendPlatformInfo")) {
        merged.getClientOptions().setSendPlatformInfo(defaultConfig.getClientOptions().getSendPlatformInfo());
      }
      if (!clientOptionsMap.containsKey("timeout")) {
        merged.getClientOptions().setTimeout(defaultConfig.getClientOptions().getTimeout());
      }
    } else {
      merged.getClientOptions().setMaxRetries(defaultConfig.getClientOptions().getMaxRetries());
      merged.getClientOptions().setSendPlatformInfo(defaultConfig.getClientOptions().getSendPlatformInfo());
      merged.getClientOptions().setTimeout(defaultConfig.getClientOptions().getTimeout());
    }
    // special handling for TextTranslationOptions not in map
    if (otherMap.get("textTranslationOptions") instanceof Map translationOptionsMap) {
      if (!translationOptionsMap.containsKey("outlineDetection")) {
        merged.getTextTranslationOptions().setOutlineDetection(defaultConfig.getTextTranslationOptions().isOutlineDetection());
      }
      if (!translationOptionsMap.containsKey("preserveFormatting")) {
        merged.getTextTranslationOptions().setPreserveFormatting(defaultConfig.getTextTranslationOptions().isPreserveFormatting());
      }
    } else {
      merged.getTextTranslationOptions().setOutlineDetection(defaultConfig.getTextTranslationOptions().isOutlineDetection());
      merged.getTextTranslationOptions().setPreserveFormatting(defaultConfig.getTextTranslationOptions().isPreserveFormatting());
    }
    if (otherMap.containsKey("glossaries")) {
      merged.setGlossaries((List<Map<String, String>>) otherMap.get("glossaries"));
    }
    return merged;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    DeeplConfiguration that = (DeeplConfiguration) o;
    return Objects.equals(apiKey, that.apiKey) && equals(clientOptions, that.clientOptions) && equals(textTranslationOptions, that.textTranslationOptions) && Objects.equals(passAsIsTargetLocales, that.passAsIsTargetLocales);
  }

  // see https://github.com/DeepLcom/deepl-java/issues/71
  private boolean equals(DeepLClientOptions self, DeepLClientOptions other) {
    if (self == other) {
      return true;
    }
    if (self == null || other == null) {
      return false;
    }
    return Objects.equals(self.getApiVersion(), other.getApiVersion()) &&
            Objects.equals(self.getAppInfo(), other.getAppInfo()) &&
            Objects.equals(self.getHeaders(), other.getHeaders()) &&
            Objects.equals(self.getMaxRetries(), other.getMaxRetries()) &&
            Objects.equals(self.getProxy(), other.getProxy()) &&
            Objects.equals(self.getSendPlatformInfo(), other.getSendPlatformInfo()) &&
            Objects.equals(self.getServerUrl(), other.getServerUrl()) &&
            Objects.equals(self.getTimeout(), other.getTimeout());
  }

  // see https://github.com/DeepLcom/deepl-java/issues/71
  private boolean equals(TextTranslationOptions self, TextTranslationOptions other) {
    if (self == other) {
      return true;
    }
    if (self == null || other == null) {
      return false;
    }
    return Objects.equals(self.getContext(), other.getContext()) &&
            Objects.equals(self.getFormality(), other.getFormality()) &&
            Objects.equals(self.getGlossaryId(), other.getGlossaryId()) &&
            Objects.equals(self.getModelType(), other.getModelType()) &&
            Objects.equals(self.getTagHandling(), other.getTagHandling()) &&
            Objects.equals(self.getIgnoreTags(), other.getIgnoreTags()) &&
            Objects.equals(self.getNonSplittingTags(), other.getNonSplittingTags()) &&
            Objects.equals(self.getSplittingTags(), other.getSplittingTags()) &&
            Objects.equals(self.getSentenceSplittingMode(), other.getSentenceSplittingMode()) &&
            Objects.equals(self.isOutlineDetection(), other.isOutlineDetection()) &&
            Objects.equals(self.isPreserveFormatting(), other.isPreserveFormatting());
  }

}
