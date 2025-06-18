package com.coremedia.labs.translation.deepl.workflow.config;

import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.convert.converter.Converter;

@Configuration
@EnableConfigurationProperties({
        DeeplConfigurationProperties.class,
})
@PropertySource(value = "classpath:META-INF/coremedia/deepl-workflow.properties")
public class DeeplSpringConfiguration {

  @Bean
  public DeeplConfigurationService deeplConfigurationService(DeeplConfigurationProperties deeplConfigurationProperties) {
    return new DeeplConfigurationService(deeplConfigurationProperties);
  }

  @Bean
  @ConfigurationPropertiesBinding
  public Converter<String, Iterable<String>> stringIterableConverter() {
    return new StringIterableConverter();
  }

}
