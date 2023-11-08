package com.coremedia.labs.translation.deepl.workflow;

import com.coremedia.cap.content.Content;
import com.coremedia.cap.content.ContentObject;
import com.coremedia.cap.user.User;
import com.coremedia.cap.workflow.Process;
import com.coremedia.cap.workflow.Task;
import com.coremedia.collaboration.project.Project;
import com.coremedia.collaboration.project.ProjectRepository;
import com.coremedia.workflow.common.util.SpringAwareLongAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.List;

public class CreateProjectAction extends SpringAwareLongAction {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private ProjectRepository projectRepository;
  private String derivedContentsVariable;
  private String masterContentObjectsVariable;
  private String subjectVariable;
  private String commentVariable;
  private String performerVariable;

  public CreateProjectAction() {
    super();
  }

  public ProjectRepository getProjectRepository() {
    return projectRepository;
  }

  public void setProjectRepository(ProjectRepository projectRepository) {
    this.projectRepository = projectRepository;
  }

  public String getDerivedContentsVariable() {
    return derivedContentsVariable;
  }

  public void setDerivedContentsVariable(String derivedContentsVariable) {
    this.derivedContentsVariable = derivedContentsVariable;
  }

  public String getMasterContentObjectsVariable() {
    return masterContentObjectsVariable;
  }

  public void setMasterContentObjectsVariable(String masterContentObjectsVariable) {
    this.masterContentObjectsVariable = masterContentObjectsVariable;
  }

  public String getSubjectVariable() {
    return subjectVariable;
  }

  public void setSubjectVariable(String subjectVariable) {
    this.subjectVariable = subjectVariable;
  }

  public String getCommentVariable() {
    return commentVariable;
  }

  public void setCommentVariable(String commentVariable) {
    this.commentVariable = commentVariable;
  }

  public String getPerformerVariable() {
    return performerVariable;
  }

  public void setPerformerVariable(String performerVariable) {
    this.performerVariable = performerVariable;
  }

  // --- LongAction interface ----------------------------------------------------------------------

  @Override
  public Object extractParameters(Task task) {
    Process process = task.getContainingProcess();

    setProjectRepository(getSpringContext().getBean(ProjectRepository.class));

    List<Content> derivedContents = process.getLinks(derivedContentsVariable);
    List<ContentObject> masterContentObjects = process.getLinksAndVersions(masterContentObjectsVariable);
    String subject = process.getString(subjectVariable);
    String comment = process.getString(commentVariable);
    User performer = process.getUser(performerVariable);
    return new Parameters(derivedContents, masterContentObjects, subject, comment, performer);
  }

  @Override
  protected Object doExecute(Object params) throws Exception {
    Parameters parameters = (Parameters) params;

    Collection<Content> derivedContents = parameters.derivedContents;

    if (derivedContents.isEmpty()) {
      return null;
    }

    LOG.debug("Create project for workflow: {}", parameters.subject);
    User user = parameters.performer;
    String projectName = "Workflow - " + parameters.subject;
    String projectDescription = parameters.comment;
    Project project = projectRepository.createProject(user, projectName, null, projectDescription, null);
    project.setContents(derivedContents);

    return project;
  }

  // Internal
  private static final class Parameters {
    public final Collection<Content> derivedContents;
    public final Collection<ContentObject> masterContentObjects;
    public final String subject;
    public final String comment;
    public final User performer;

    public Parameters(final Collection<Content> derivedContents,
                      final Collection<ContentObject> masterContentObjects,
                      final String subject,
                      final String comment,
                      final User performer) {
      this.derivedContents = derivedContents;
      this.masterContentObjects = masterContentObjects;
      this.subject = subject;
      this.comment = comment;
      this.performer = performer;
    }
  }

}
