package com.coremedia.labs.translation.deepl.workflow;

import com.coremedia.cap.content.Content;
import com.coremedia.cap.content.ContentObject;
import com.coremedia.cap.multisite.ContentObjectSiteAspect;
import com.coremedia.cap.multisite.Site;
import com.coremedia.cap.multisite.SitesService;
import com.coremedia.cap.struct.Struct;
import com.coremedia.cap.util.StructUtil;
import com.coremedia.cap.workflow.Process;
import com.coremedia.cap.workflow.Task;
import com.coremedia.workflow.common.util.SpringAwareLongAction;
import edu.umd.cs.findbugs.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.coremedia.labs.translation.deepl.workflow.ConfigProperties.KEY_DEEPL_ROOT;
import static java.lang.invoke.MethodHandles.lookup;

public abstract class DeeplAction extends SpringAwareLongAction {

  private static final Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

  private static final String LOCAL_SETTINGS = "localSettings";
  private static final String LINKED_SETTINGS = "linkedSettings";
  private static final String CMSETTINGS_SETTINGS = "settings";

  protected String derivedContentsVariable;
  protected String masterContentObjectsVariable;

  /**
   * Return the name of the process variable that stores the list of contents
   * for which a translation should be generated.
   *
   * @return the name of the process variable
   */
  public String getDerivedContentsVariable() {
    return derivedContentsVariable;
  }

  /**
   * Return the name of the process variable that stores the list of contents
   * for which a translation should be generated.
   *
   * @param derivedContentsVariable the name of the process variable
   */
  public void setDerivedContentsVariable(String derivedContentsVariable) {
    this.derivedContentsVariable = derivedContentsVariable;
  }

  /**
   * Return the name of the process variable containing the source contents objects.
   *
   * @return the name of the process variable
   */
  public String getMasterContentObjectsVariable() {
    return masterContentObjectsVariable;
  }

  /**
   * Set the name of the process variable containing the source contents objects.
   *
   * @param masterContentObjectsVariable the name of the process variable
   */
  public void setMasterContentObjectsVariable(String masterContentObjectsVariable) {
    this.masterContentObjectsVariable = masterContentObjectsVariable;
  }

  // --- Internal ---

  protected Site getMasterSite(Collection<? extends ContentObject> masterContents) {
    SitesService sitesService = getSitesService();
    return masterContents.stream()
            .map(sitesService::getSiteAspect)
            .map(ContentObjectSiteAspect::getSite)
            .filter(Objects::nonNull)
            .findAny()
            .orElseThrow(() -> new IllegalStateException("No master site found"));
  }

  SitesService getSitesService() {
    ApplicationContext springContext = getSpringContext();
    return springContext.getBean(SitesService.class);
  }


  // --- Settings handling ---

  Map<String, Object> getDefaultDeeplSettings() {
    return new HashMap<String,Object>(getSpringContext().getBean("deeplConfigurationProperties", Map.class));
  }

  Map<String, Object> getDeeplSettingForSite(Site site) {
    Map<String, Object> defaults = getDefaultDeeplSettings();
    Map<String, Object> mergedSettings = new HashMap<>(defaults);

    Content siteRootDocument = site.getSiteRootDocument();
    Map<String, Object> siteSpecificSetting = getDeeplSettingsForContent(siteRootDocument);
    if (!siteSpecificSetting.isEmpty()) {
      mergedSettings.putAll(siteSpecificSetting);
    }

    return Collections.unmodifiableMap(mergedSettings);
  }

  Map<String, Object> getDeeplSettingsForContent(Content content) {
    Struct localSettings = getStruct(content, LOCAL_SETTINGS);
    Struct struct = StructUtil.mergeStructList(
            localSettings,
            content.getLinks(LINKED_SETTINGS)
                    .stream()
                    .map(link -> getStruct(link, CMSETTINGS_SETTINGS))
                    .collect(Collectors.toList())
    );
    if (struct != null) {
      Object value = struct.get(KEY_DEEPL_ROOT);
      if (value instanceof Struct) {
        return ((Struct) value).toNestedMaps();
      }
    }

    return Collections.emptyMap();
  }

  @Nullable
  private static Struct getStruct(Content content, String name) {
    if (content != null && content.isInProduction()) {
      return content.getStruct(name);
    }
    return null;
  }

}
