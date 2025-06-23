package com.coremedia.labs.translation.deepl.studio.validation;

import com.coremedia.cap.multisite.SitesService;
import com.coremedia.cap.workflow.TaskState;
import com.coremedia.labs.translation.deepl.workflow.config.DeeplConfigurationConfiguration;
import com.coremedia.labs.translation.deepl.workflow.config.DeeplConfigurationService;
import com.coremedia.rest.cap.workflow.validation.WorkflowValidator;
import com.coremedia.rest.cap.workflow.validation.configuration.TranslationWorkflowValidationConfiguration;
import com.coremedia.rest.cap.workflow.validation.model.ValidationTask;
import com.coremedia.rest.cap.workflow.validation.model.WorkflowStartValidators;
import com.coremedia.rest.cap.workflow.validation.model.WorkflowTaskValidators;
import com.coremedia.rest.cap.workflow.validation.model.WorkflowValidatorsModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.coremedia.rest.cap.workflow.validation.configuration.TranslationWorkflowValidationConfiguration.*;

@AutoConfiguration
@Import({DeeplConfigurationConfiguration.class,
        TranslationWorkflowValidationConfiguration.class})
public class DeeplWorkflowValidationAutoConfiguration {

  public static final String TRANSLATION_DEEPL_VALIDATOR_KEY = "TranslationDeepl";
  public static final String REVIEW = "Review";

  @Bean
  WorkflowValidatorsModel translationDeeplWFValidators(@Qualifier(TRANSLATION_START_VALIDATORS) WorkflowStartValidators translationStartValidators,
                                                       @Qualifier(TRANSLATION_WFNOT_RUNNING) List<WorkflowValidator> translationWFNotRunning,
                                                       @Qualifier(TRANSLATION_WFRUNNING) List<WorkflowValidator> translationWFRunning,
                                                       @Qualifier(TASK_ERROR_VALIDATOR) WorkflowValidator taskErrorValidator,
                                                       DeeplConfigurationService deeplConfigurationService,
                                                       SitesService sitesService) {
    ValidationTask runningTask = new ValidationTask(TRANSLATE_TASK_NAME, TaskState.RUNNING);
    ValidationTask waitingTask = new ValidationTask(TRANSLATE_TASK_NAME, TaskState.ACTIVATED);
    ValidationTask reviewTask = new ValidationTask(REVIEW);

    WorkflowTaskValidators taskValidators = new WorkflowTaskValidators(
            Map.of(runningTask, translationWFRunning,
                    waitingTask, translationWFNotRunning,
                    // show any (non-escalating) errors during Review task
                    reviewTask, List.of(taskErrorValidator)
            )
    );

    List<WorkflowValidator> workflowValidators = new ArrayList<>();
    workflowValidators.add(new DeeplSupportedLanguagesValidator(deeplConfigurationService, sitesService));
    workflowValidators.addAll(translationStartValidators.getWorkflowValidators());
    WorkflowStartValidators deeplStartValidators = new WorkflowStartValidators(
            translationStartValidators.getWorkflowValidationPreparation(), workflowValidators
    );

    return new WorkflowValidatorsModel(TRANSLATION_DEEPL_VALIDATOR_KEY, taskValidators, deeplStartValidators);
  }
}
