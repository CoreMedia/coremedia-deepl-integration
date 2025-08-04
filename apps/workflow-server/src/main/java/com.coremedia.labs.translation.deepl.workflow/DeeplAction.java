package com.coremedia.labs.translation.deepl.workflow;

import com.coremedia.cap.common.Blob;
import com.coremedia.cap.content.Content;
import com.coremedia.cap.content.ContentObject;
import com.coremedia.cap.multisite.ContentObjectSiteAspect;
import com.coremedia.cap.multisite.Site;
import com.coremedia.cap.multisite.SitesService;
import com.coremedia.cap.translate.xliff.XliffImportResultCode;
import com.coremedia.cap.workflow.Process;
import com.coremedia.cap.workflow.Task;
import com.coremedia.cap.workflow.plugin.ActionResult;
import com.coremedia.rest.validation.Severity;
import com.coremedia.workflow.common.util.SpringAwareLongAction;
import com.deepl.api.*;
import com.google.common.annotations.VisibleForTesting;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import jakarta.activation.MimeType;
import jakarta.activation.MimeTypeParseException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serial;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;

import static java.lang.invoke.MethodHandles.lookup;
import static java.util.Objects.requireNonNull;

public abstract class DeeplAction<P, R>  extends SpringAwareLongAction {
  private static final Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

  @Serial
  private static final long serialVersionUID = -2590066452420203850L;

  private static final MimeType MIME_TYPE_JSON = mimeType("application/json");
  private static final Gson contentObjectReturnsIdGson = new GsonBuilder()
          .enableComplexMapKeySerialization()
          .registerTypeHierarchyAdapter(Content.class, new ContentObjectSerializer())
          .create();

  protected String derivedContentsVariable;
  protected String masterContentObjectsVariable;
  private String issuesVariable;

  // --- construct and configure ----------------------------------------------------------------------

  DeeplAction(boolean rethrowResultException) {
    super(rethrowResultException);
  }


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

  /**
   * Sets the name of the blob process variable to store a JSON blob with errors that happened when interacting
   * with the translation service, or null if no such errors occurred. Studio's TaskErrorValidator
   * can then display these errors as task issues. The JSON data structure is a serialized map from severity to
   * map of error code to list of affected contents, i.e. {@code Map<Severity, Map<String, List<Content>>>}.
   *
   * @param issuesVariable blob workflow variable name
   */
  @SuppressWarnings({"unused", "WeakerAccess"}) // set from workflow definition
  public void setIssuesVariable(String issuesVariable) {
    this.issuesVariable = requireNonNull(issuesVariable);
  }

  // --- LongAction interface ----------------------------------------------------------------------

  @Override
  public final Parameters<P> extractParameters(Task task) {
    Process process = task.getContainingProcess();
    List<ContentObject> masterContentObjects = process.getLinksAndVersions(getMasterContentObjectsVariable());
    P extendedParameters = doExtractParameters(task);
    return new Parameters<>(extendedParameters, masterContentObjects);
  }

  @Override
  protected final Result<R> doExecute(Object params) {
    if (params == null) {
      // skip
      return null;
    }
    @SuppressWarnings("unchecked" /* per interface contract: params is the return value of #extractParameters */)
    Parameters<P> parameters = (Parameters<P>) params;
    Result<R> result = new Result<>();
    // maps error codes to affected contents; list of contents may be empty for some errors */
    Map<String, List<Content>> issues = new HashMap<>();
    try {
      // call subclass implementation and store the result as result.extendedResult
      Consumer<R> resultConsumer = r -> result.extendedResult = Optional.of(r);
      doExecuteDeeplAction(parameters.extendedParameters, resultConsumer, issues);
    } catch (AuthorizationException ae) {
      throw new DeeplWorkflowException(DeeplWorkflowErrorCodes.AUTHORIZATION_ERROR, ae.getMessage(), ae);
    } catch (QuotaExceededException qae) {
      throw new DeeplWorkflowException(DeeplWorkflowErrorCodes.QUOTA_EXCEEDED_ERROR, qae.getMessage(), qae);
    } catch (TooManyRequestsException tmre) {
      throw new DeeplWorkflowException(DeeplWorkflowErrorCodes.TOO_MANY_REQUESTS_ERROR, tmre.getMessage(), tmre);
    } catch (ConnectionException ce) {
      throw new DeeplWorkflowException(DeeplWorkflowErrorCodes.CONNECTION_ERROR, ce.getMessage(), ce);
    } catch (NotFoundException nfe) {
      throw new DeeplWorkflowException(DeeplWorkflowErrorCodes.NOT_FOUND_ERROR, nfe.getMessage(), nfe);
    } catch (Exception e) {
      throw new DeeplWorkflowException(DeeplWorkflowErrorCodes.UNKNOWN_ERROR, e.getMessage(), e);
    }
    result.issues = issuesAsJsonBlob(issues);
    return result;
  }

  @Override
  public final ActionResult storeResult(Task task, Object result) {
    checkNotAborted(task);
    if (result instanceof Exception) {
      return storeResultException(task, (Exception) result);
    }
    if (result == null) {
      // skip
      return ActionResult.SUCCESSFUL;
    }
    @SuppressWarnings("unchecked" /* per interface contract: result is the return value of #doExecute */)
    Result<R> r = (Result<R>) result;
    Process process = task.getContainingProcess();
    if (StringUtils.isNotBlank(issuesVariable) && r.issues != null) {
      process.set(issuesVariable, r.issues);
    }
    Object resultValue = r.extendedResult
            .map(extendedResult -> doStoreResult(task, extendedResult))
            .orElse(null);
    return super.storeResult(task, resultValue);
  }

  // --- Methods to be implemented / overridden by concrete subclass -------------------------------

  abstract P doExtractParameters(Task task);

  abstract void doExecuteDeeplAction(P parameters,
                                          Consumer<? super R> resultConsumer,
                                          Map<String, List<Content>> issues) throws Exception;

  @Nullable
  Object doStoreResult(Task task, R result) {
    return result;
  }

  // --- Helper methods for subclasses ----------------------------------------

  SitesService getSitesService() {
    return getSpringContext().getBean(SitesService.class);
  }

  static MimeType mimeType(String mimeTypeString) {
    try {
      return new MimeType(mimeTypeString);
    } catch (MimeTypeParseException e) {
      throw new IllegalArgumentException("Cannot parse mime-type: '" + mimeTypeString + "'.", e);
    }
  }

  protected Site getMasterSite(Collection<? extends ContentObject> masterContents) {
    SitesService sitesService = getSitesService();
    return masterContents.stream()
            .map(sitesService::getSiteAspect)
            .map(ContentObjectSiteAspect::getSite)
            .filter(Objects::nonNull)
            .findAny()
            .orElseThrow(() -> new IllegalStateException("No master site found"));
  }

  // --- Internal ---

  @VisibleForTesting
  @Nullable
  Blob issuesAsJsonBlob(Map<String, List<Content>> issues) {
    if (issues.isEmpty()) {
      return null;
    }
    // all issues should have the severity ERROR when displayed in Studio
    Map<Severity, Map<String, List<Content>>> studioIssues = Collections.singletonMap(Severity.ERROR, issues);
    byte[] bytes = issuesAsJsonString(studioIssues).getBytes(StandardCharsets.UTF_8);
    return getConnection().getBlobService().fromBytes(bytes, MIME_TYPE_JSON);
  }

  @NonNull
  @VisibleForTesting
  static String issuesAsJsonString(Map<Severity, Map<String, List<Content>>> issues) {
    Type typeToken = new TypeToken<Map<Severity, Map<XliffImportResultCode, List<Content>>>>() {
    }.getType();
    return contentObjectReturnsIdGson.toJson(issues, typeToken);
  }

  private static class ContentObjectSerializer implements JsonSerializer<ContentObject> {
    @Override
    public JsonElement serialize(ContentObject contentObject, Type type, JsonSerializationContext jsonSerializationContext) {
      if (contentObject == null) {
        return JsonNull.INSTANCE;
      }
      return new JsonPrimitive(contentObject.getId());
    }
  }

  public record Parameters<P>(P extendedParameters, Collection<ContentObject> masterContentObjects) {
  }

  protected static final class Result<R> {
    /**
     * Holds the result from {@link #doExecuteDeeplAction}, empty for no result
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType") // suppress warning for non-typical usage of Optional
            Optional<R> extendedResult = Optional.empty();
    /**
     * JSON with a map from studio severity to a map of error codes to possibly empty list of affected contents
     */
    Blob issues;
  }
}
