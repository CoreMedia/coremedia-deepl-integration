import { workflowLocalizationRegistry } from "@coremedia/studio-client.workflow-plugin-models/WorkflowLocalizationRegistry";
import { workflowPlugins } from "@coremedia/studio-client.workflow-plugin-models/WorkflowPluginRegistry";
import Deepl_properties from "./Deepl_properties";

const WORKFLOW_NAME:string = "TranslationDeepl";

interface DeeplViewModel {

}

workflowPlugins._.addTranslationWorkflowPlugin<DeeplViewModel>({
  workflowType: "TRANSLATION",
  workflowName: WORKFLOW_NAME,
  createWorkflowPerTargetSite: false,
});

workflowLocalizationRegistry._.addLocalization(WORKFLOW_NAME, {
  displayName: Deepl_properties.TranslationDeepl_displayName,
  description: Deepl_properties.TranslationDeepl_description,
});

workflowLocalizationRegistry._.addIssuesLocalization({
  SUCCESS: {
    singular: Deepl_properties.SUCCESS_singular_text,
    plural: Deepl_properties.SUCCESS_plural_text,
  }
});
