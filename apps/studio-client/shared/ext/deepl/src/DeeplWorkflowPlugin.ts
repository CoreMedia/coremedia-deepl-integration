import {
  workflowLocalizationRegistry
} from "@coremedia/studio-client.workflow-plugin-models/WorkflowLocalizationRegistry";
import { workflowPlugins } from "@coremedia/studio-client.workflow-plugin-models/WorkflowPluginRegistry";
import Deepl_properties from "./Deepl_properties";
import deeplWorkflowIcon from "./icons/deepl-workflow.svg";
import {
  Binding,
  CheckField, StartWorkflowFormExtension,
  TranslationWorkflowPlugin
} from "@coremedia/studio-client.workflow-plugin-models/CustomWorkflowApi";
import editorContext from "@coremedia/studio-client.main.editor-components/sdk/editorContext";
import StudioConfigurationUtil
  from "@coremedia/studio-client.ext.cap-base-components/util/config/StudioConfigurationUtil";
import additionalWorkflowIssues
  from "@coremedia/studio-client.ext.workflow-components/components/validation/issues/additionalWorkflowIssues";
import { getLocalizer } from "@coremedia/studio-client.i18n-models";

const WORKFLOW_NAME: string = "TranslationDeepl";
const WORKFLOW_NAME_DIRECT: string = "TranslationDeeplDirect";
const DEEPL_SETTINGS_BUNDLE: string = "Translation Services/DeepL";
const DEEPL_STRUCT_NAME: string = "deepl";

interface DeeplViewModel {
  createProject?: boolean;
}

const getTranslationWorkflowPlugin = async (): Promise<TranslationWorkflowPlugin> => {
  const localizer = await getLocalizer(Deepl_properties);
  return {
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
    startWorkflowFormExtension: StartWorkflowFormExtension<DeeplViewModel>({

      computeViewModel(): DeeplViewModel {
        return { createProject: getCreateProjectFlagDefault() };
      },

      saveViewModel(viewModel: DeeplViewModel): Record<string, any> {
        return { createProject: viewModel.createProject };
      },

      fields: [
        CheckField({
          label: localizer("TranslationDeepl_field_createProject_label"),
          tooltip: localizer("TranslationDeepl_field_createProject_tooltip"),
          value: Binding("createProject")
        })
      ]
    }),
  };
};

getTranslationWorkflowPlugin().then((workflowPlugin) => {
  workflowPlugins._.addTranslationWorkflowPlugin(workflowPlugin);
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


const getDirectTranslationWorkflowPlugin = async (): Promise<TranslationWorkflowPlugin> => {
  const localizer = await getLocalizer(Deepl_properties);
  return {
    workflowType: "TRANSLATION",
    workflowName: WORKFLOW_NAME_DIRECT,
    createWorkflowPerTargetSite: false,

    startWorkflowFormExtension: StartWorkflowFormExtension<DeeplViewModel>({

      computeViewModel(): DeeplViewModel {
        return { createProject: getCreateProjectFlagDefault() };
      },

      saveViewModel(viewModel: DeeplViewModel): Record<string, any> {
        return { createProject: viewModel.createProject };
      },

      fields: [
        CheckField({
          label: localizer("TranslationDeepl_field_createProject_label"),
          tooltip: localizer("TranslationDeepl_field_createProject_tooltip"),
          value: Binding("createProject")
        })
      ]
    }),
  };
};


getDirectTranslationWorkflowPlugin().then((workflowPlugin) => {
  workflowPlugins._.addTranslationWorkflowPlugin(workflowPlugin);
});

workflowLocalizationRegistry._.addLocalization(WORKFLOW_NAME_DIRECT, {
  displayName: Deepl_properties.TranslationDeeplDirect_displayName,
  description: Deepl_properties.TranslationDeeplDirect_description,
  svgIcon: deeplWorkflowIcon,
});

workflowLocalizationRegistry._.addIssuesLocalization({
  "DEEPL-WF-10000": Deepl_properties["DEEPL-WF-10000_text"],
  "DEEPL-WF-20000": Deepl_properties["DEEPL-WF-20000_text"],
  "DEEPL-WF-20001": Deepl_properties["DEEPL-WF-20001_text"],
  "DEEPL-WF-20002": Deepl_properties["DEEPL-WF-20002_text"],
  "DEEPL-WF-20003": Deepl_properties["DEEPL-WF-20003_text"],
  "DEEPL-WF-20004": Deepl_properties["DEEPL-WF-20004_text"],
  "DEEPL-WF-50000": Deepl_properties["DEEPL-WF-50000_text"],
  failedLanguageValidation: Deepl_properties.failedLanguageValidation_text,
  unsupportedSourceLocales: {
    singular: Deepl_properties.ERROR_singular_text,
    plural:Deepl_properties.ERROR_plural_text,
  },
  unsupportedTargetLocales: {
    singular: Deepl_properties.WARN_target_singular_text,
    plural:Deepl_properties.WARN_target_plural_text,
  },

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
    text: "Site is not available for Translation",
  }
];

const SITES_RELATED_ISSUES = [
  {
    wfIssuesCode: "unsupportedTargetLocales",
    wfIssuesPriority: 3,
    text: "Site is not available for Translation",
  }
];

addValidationStateMapping("contentRelatedIssueCodes", CONTENT_RELATED_ISSUES);
addValidationStateMapping("sitesRelatedIssues", SITES_RELATED_ISSUES);

function addValidationStateMapping(issueGroupName: string, issues: Array<{
  wfIssuesCode: string,
  wfIssuesPriority: number
}>): void {
  let wfIssues = additionalWorkflowIssues._.get(issueGroupName);
  if (wfIssues) {
    wfIssues.concat(issues);
  } else {
    additionalWorkflowIssues._.set(issueGroupName, issues);
  }
}

