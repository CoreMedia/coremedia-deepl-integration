package com.coremedia.labs.translation.deepl.workflow;

import com.coremedia.blueprint.workflow.actions.CreateProjectActionConfiguration;
import com.coremedia.cap.translate.xliff.config.XliffExporterConfiguration;
import com.coremedia.cap.translate.xliff.config.XliffImporterConfiguration;
import com.coremedia.translate.item.TranslateItemConfiguration;
import com.coremedia.translate.workflow.DefaultTranslationWorkflowDerivedContentsStrategy;
import com.coremedia.translate.workflow.TranslationWorkflowDerivedContentsStrategy;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

import java.util.HashMap;
import java.util.Map;

@AutoConfiguration
@Import({
        XliffImporterConfiguration.class,
        XliffExporterConfiguration.class,
        TranslateItemConfiguration.class,
        CreateProjectActionConfiguration.class})
@PropertySource(value = "classpath:META-INF/coremedia/deepl-workflow.properties")
@DefaultAnnotation(NonNull.class)
public class TranslateDeeplConfiguration {

  @Bean
  DeeplTranslationService deeplTranslationService() {
    return new DeeplTranslationService();
  }

  /**
   * A strategy for extracting derived contents from the default translation.xml workflow definition.
   *
   * @return deeplTranslationWorkflowDerivedContentsStrategy
   */
  @Bean
  TranslationWorkflowDerivedContentsStrategy deeplTranslationWorkflowDerivedContentsStrategy() {
    DefaultTranslationWorkflowDerivedContentsStrategy deeplTranslationWorkflowDerivedContentsStrategy = new DefaultTranslationWorkflowDerivedContentsStrategy();
    deeplTranslationWorkflowDerivedContentsStrategy.setProcessDefinitionName("TranslationDeepl");
    return deeplTranslationWorkflowDerivedContentsStrategy;
  }

  @ConfigurationProperties(prefix = "deepl")
  @Bean
  public Map<String, Object> deeplConfigurationProperties() {
    return new HashMap<>();
  }

}
