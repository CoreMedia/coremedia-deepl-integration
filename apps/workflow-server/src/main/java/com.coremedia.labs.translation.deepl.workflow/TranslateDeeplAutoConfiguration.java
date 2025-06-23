package com.coremedia.labs.translation.deepl.workflow;

import com.coremedia.blueprint.workflow.actions.CreateProjectActionConfiguration;
import com.coremedia.cap.content.ContentRepository;
import com.coremedia.cap.translate.xliff.config.XliffExporterConfiguration;
import com.coremedia.cap.translate.xliff.config.XliffImporterConfiguration;
import com.coremedia.labs.translation.deepl.workflow.config.DeeplConfigurationConfiguration;
import com.coremedia.labs.translation.deepl.workflow.config.DeeplConfigurationProperties;
import com.coremedia.translate.item.TranslateItemConfiguration;
import com.coremedia.translate.workflow.DefaultTranslationWorkflowDerivedContentsStrategy;
import com.coremedia.translate.workflow.TranslationWorkflowDerivedContentsStrategy;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;

@AutoConfiguration
@Import({
        DeeplConfigurationConfiguration.class,
        XliffImporterConfiguration.class,
        XliffExporterConfiguration.class,
        TranslateItemConfiguration.class,
        CreateProjectActionConfiguration.class})
@DefaultAnnotation(NonNull.class)
public class TranslateDeeplAutoConfiguration {

  @Bean
  @Scope("prototype")
  DeeplTranslationService deeplTranslationService(DeeplConfigurationProperties deepLConfigurationProperties,
                                                  ContentRepository contentRepository) {
    return new DeeplTranslationService(deepLConfigurationProperties, contentRepository);
  }

  /**
   * A strategy for extracting derived contents from the default translation.xml workflow definition.
   *
   * @return deeplTranslationWorkflowDerivedContentsStrategy
   */
  @Bean
  @SuppressWarnings("unused")
  TranslationWorkflowDerivedContentsStrategy deeplTranslationWorkflowDerivedContentsStrategy() {
    DefaultTranslationWorkflowDerivedContentsStrategy deeplTranslationWorkflowDerivedContentsStrategy = new DefaultTranslationWorkflowDerivedContentsStrategy();
    // artificial use of InitializingBean as workaround for issues with spring-beans dependency after using it in tests
    InitializingBean initializingBean = deeplTranslationWorkflowDerivedContentsStrategy;
    deeplTranslationWorkflowDerivedContentsStrategy.setProcessDefinitionName("TranslationDeepl");
    return deeplTranslationWorkflowDerivedContentsStrategy;
  }

}
