package com.coremedia.labs.translation.deepl.workflow.config;

import org.springframework.core.convert.converter.Converter;

import java.util.Arrays;

/**
 * Spring Converter to support setting Iterable<String> properties from application properties.
 */
public class StringIterableConverter implements Converter<String, Iterable<String>> {

  @Override
  public Iterable<String> convert(String source) {
    return Arrays.asList(source.split("\\s*,\\s*"));
  }
}
