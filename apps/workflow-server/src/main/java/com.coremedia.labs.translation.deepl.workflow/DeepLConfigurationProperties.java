package com.coremedia.labs.translation.deepl.workflow;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

// TODO: consolidate with existing loose "deepl" configuration...
@ConfigurationProperties(prefix = "deepl.config")
public class DeepLConfigurationProperties {
  /**
   * List of target locales that should be passed as-is as DeepL target language.
   * See <a href="https://developers.deepl.com/docs/getting-started/supported-languages">Supported Languages.</a>.
   */
  private List<String> passAsIsTargetLocales = List.of("en-US", "en-GB", "pt-PT", "pt-BR", "zh-HANS", "zh-HANT");

  public List<String> getPassAsIsTargetLocales() {
    return passAsIsTargetLocales;
  }

  public void setPassAsIsTargetLocales(List<String> passAsIsTargetLocales) {
    this.passAsIsTargetLocales = passAsIsTargetLocales;
  }
}
