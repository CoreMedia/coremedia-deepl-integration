import { workflowLocalizationRegistry } from "@coremedia/studio-client.workflow-plugin-models/WorkflowLocalizationRegistry";
import { workflowPlugins } from "@coremedia/studio-client.workflow-plugin-models/WorkflowPluginRegistry";
import Deepl_properties from "./Deepl_properties";
import deeplWorkflowIcon from "./icons/deepl-workflow.svg";

const WORKFLOW_NAME:string = "TranslationDeepl";

interface DeeplViewModel {

}

workflowPlugins._.addTranslationWorkflowPlugin<DeeplViewModel>({
  workflowType: "TRANSLATION",
  workflowName: WORKFLOW_NAME,
  createWorkflowPerTargetSite: false,
  nextStepVariable: "translationAction",
  transitions: [
    {
      task: "Review",
      defaultNextTask: "finishTranslation",
      nextSteps: [
        {
          name: "rollbackTranslation",
          allowAlways: true,
        },
        {
          name: "finishTranslation",
          allowAlways: true,
        },
      ],
    },
  ],

});

workflowLocalizationRegistry._.addLocalization(WORKFLOW_NAME, {
  displayName: Deepl_properties.TranslationDeepl_displayName,
  description: Deepl_properties.TranslationDeepl_description,
  svgIcon: deeplWorkflowIcon,
  states: {
    finishTranslation: Deepl_properties.TranslationDeepl_state_finishTranslation_displayName,
    rollbackTranslation: Deepl_properties.TranslationDeepl_state_rollbackTranslation_displayName
  }
});

workflowLocalizationRegistry._.addIssuesLocalization({
  SUCCESS: {
    singular: Deepl_properties.SUCCESS_singular_text,
    plural: Deepl_properties.SUCCESS_plural_text,
  }
});
