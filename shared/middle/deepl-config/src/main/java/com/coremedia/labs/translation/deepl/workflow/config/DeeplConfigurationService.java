package com.coremedia.labs.translation.deepl.workflow.config;

import com.coremedia.cap.content.Content;
import com.coremedia.cap.multisite.Site;
import com.coremedia.cap.struct.Struct;
import com.coremedia.cap.util.StructUtil;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@DefaultAnnotation(NonNull.class)
public class DeeplConfigurationService {
  private static final String LOCAL_SETTINGS = "localSettings";
  private static final String LINKED_SETTINGS = "linkedSettings";
  private static final String CMSETTINGS_SETTINGS = "settings";
  private static final String KEY_DEEPL_ROOT = "deepl";

  private final DeeplConfiguration defaultConfig;

  public DeeplConfigurationService(DeeplConfigurationProperties defaultConfig) {
    this.defaultConfig = defaultConfig;
  }

  public DeeplConfiguration getDeeplConfigurationForSite(Site site) {
    Content siteRootDocument = site.getSiteRootDocument();
    if(siteRootDocument == null) {
      return defaultConfig;
    }
    Map<String, Object> siteConfig = getDeeplConfigurationForContent(siteRootDocument);
    return DeeplConfiguration.merge(defaultConfig, siteConfig);
  }

  protected Map<String, Object> getDeeplConfigurationForContent(Content content) {
    Struct localSettings = getStruct(content, LOCAL_SETTINGS);
    Struct struct = StructUtil.mergeStructList(
            localSettings,
            content.getLinks(LINKED_SETTINGS)
                    .stream()
                    .map(link -> getStruct(link, CMSETTINGS_SETTINGS))
                    .collect(Collectors.toList())
    );

    Map<String, Object> structSettings = new HashMap<>();

    if (struct != null) {
      Object value = struct.get(KEY_DEEPL_ROOT);
      if (value instanceof Struct) {
        structSettings = ((Struct) value).toNestedMaps();
      }
    }

    return structSettings;
  }

  @Nullable
  private static Struct getStruct(Content content, String name) {
    if (content != null && content.isInProduction()) {
      return content.getStruct(name);
    }
    return null;
  }

}
