package com.coremedia.labs.translation.deepl.workflow;

import com.coremedia.cap.content.ContentObject;
import com.coremedia.cap.multisite.ContentObjectSiteAspect;
import com.coremedia.cap.multisite.Site;
import com.coremedia.cap.multisite.SitesService;
import com.coremedia.workflow.common.util.SpringAwareLongAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.util.Collection;
import java.util.Objects;

import static java.lang.invoke.MethodHandles.lookup;

public abstract class DeeplAction extends SpringAwareLongAction {

  private static final Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

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

}
