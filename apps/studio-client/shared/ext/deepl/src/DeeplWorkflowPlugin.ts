import {
  workflowLocalizationRegistry
} from "@coremedia/studio-client.workflow-plugin-models/WorkflowLocalizationRegistry";
import {workflowPlugins} from "@coremedia/studio-client.workflow-plugin-models/WorkflowPluginRegistry";
import Deepl_properties from "./Deepl_properties";
import deeplWorkflowIcon from "./icons/deepl-workflow.svg";
import {Binding, CheckField} from "@coremedia/studio-client.workflow-plugin-models/CustomWorkflowApi";
import editorContext from "@coremedia/studio-client.main.editor-components/sdk/editorContext";
import StudioConfigurationUtil
  from "@coremedia/studio-client.ext.cap-base-components/util/config/StudioConfigurationUtil";
import additionalWorkflowIssues
  from "@coremedia/studio-client.ext.workflow-components/components/validation/issues/additionalWorkflowIssues";

const WORKFLOW_NAME: string = "TranslationDeepl";
const DEEPL_SETTINGS_BUNDLE: string = "Translation Services/DeepL";
const DEEPL_STRUCT_NAME: string = "deepl";

interface DeeplViewModel {
  createProject?: boolean;
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
  startWorkflowFormExtension: {

    computeViewModel(): DeeplViewModel {
      return {createProject: getCreateProjectFlagDefault()};
    },

    saveViewModel(viewModel: DeeplViewModel): Record<string, any> {
      return {createProject: viewModel.createProject};
    },

    fields: [
      CheckField({
        label: Deepl_properties.TranslationDeepl_field_createProject_label,
        tooltip: Deepl_properties.TranslationDeepl_field_createProject_tooltip,
        value: Binding("createProject")
      })
    ]
  }
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




function getCreateProjectFlagDefault(): boolean {
  let preferredSite = editorContext._.getSitesService().getPreferredSite();
  const deeplSettings = StudioConfigurationUtil.getConfiguration(DEEPL_SETTINGS_BUNDLE, DEEPL_STRUCT_NAME, preferredSite);
  if (deeplSettings) {
    return deeplSettings.get("createProject");
  }
  return undefined;
}

// Add validation issue mappings for ui components in workflow dialog (see: WorkflowComponentValidationStateUtil.ts)
const CONTENT_RELATED_ISSUES = [
  {
    wfIssuesCode: "unsupportedSourceLocales",
    wfIssuesPriority: 2,
  }
];

const SITES_RELATED_ISSUES = [
  {
    wfIssuesCode: "unsupportedTargetLocales",
    wfIssuesPriority: 3,
  }
];

addValidationStateMapping("contentRelatedIssueCodes", CONTENT_RELATED_ISSUES);
addValidationStateMapping("sitesRelatedIssues", SITES_RELATED_ISSUES);

function addValidationStateMapping(issueGroupName: string, issues: Array<{wfIssuesCode: string, wfIssuesPriority: number}>): void {
  let wfIssues = additionalWorkflowIssues._.get(issueGroupName);
  if (wfIssues) {
    wfIssues.concat(issues);
  } else {
    additionalWorkflowIssues._.set(issueGroupName, issues);
  }
}

