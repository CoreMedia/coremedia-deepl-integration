import Deepl_properties from "./Deepl_properties";
import {
  Binding,
  CheckField,
  StartWorkflowFormExtension,
  TranslationWorkflowPlugin,
  WorkflowIssuesLocalization, WorkflowLocalization, workflowLocalizationRegistry, workflowPlugins
} from "@coremedia/studio-client.workflow-plugin-models";
import { getLocalizer, registerLocale } from "@coremedia/studio-client.i18n-models";
import deeplWorkflowIcon from "./icons/deepl-workflow.svg";

// Register localization bundles
registerLocale(Deepl_properties, "de", async () => {
  await import("./Deepl_de_properties");
});
registerLocale(Deepl_properties, "ja", async () => {
  await import("./Deepl_ja_properties");
});

const DEEPL_REVIEWED_TRANSLATION_WORKFLOW_NAME: string = "TranslationDeepl";
const DEEPL_DIRECT_TRANSLATION_WORKFLOW: string = "TranslationDeeplDirect";

interface DeeplViewModel {
  createProject?: boolean;
}

// --- DEEPL REVIEWED WORKFLOW ---

const getDeeplReviewedTranslationWorkflowPlugin = async (): Promise<TranslationWorkflowPlugin> => {
  const localizer = await getLocalizer(Deepl_properties);
  return {
    workflowType: "TRANSLATION",
    workflowName: DEEPL_REVIEWED_TRANSLATION_WORKFLOW_NAME,
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
        // return { createProject: getCreateProjectFlagDefault() };
        return { createProject: false };
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

getDeeplReviewedTranslationWorkflowPlugin().then((workflowPlugin) => {
  workflowPlugins._.addTranslationWorkflowPlugin(workflowPlugin);
});

const getDeeplReviewedTranslationWorkflowLocalization = async (): Promise<WorkflowLocalization> => {
  return {
    displayName: Deepl_properties.TranslationDeepl_displayName,
    description: Deepl_properties.TranslationDeepl_description,
    svgIcon: deeplWorkflowIcon,
    states: {
      finishTranslation: Deepl_properties.TranslationDeepl_state_finishTranslation_displayName,
      rollbackTranslation: Deepl_properties.TranslationDeepl_state_rollbackTranslation_displayName
    }
  };
};

getDeeplReviewedTranslationWorkflowLocalization().then((workflowLocalization) => {
  workflowLocalizationRegistry._.addLocalization(DEEPL_REVIEWED_TRANSLATION_WORKFLOW_NAME, workflowLocalization);
});

// --- DEEPL DIRECT WORKFLOW ---

const getDeeplDirectTranslationWorkflowPlugin = async (): Promise<TranslationWorkflowPlugin> => {
  const localizer = await getLocalizer(Deepl_properties);
  return {
    workflowType: "TRANSLATION",
    workflowName: DEEPL_DIRECT_TRANSLATION_WORKFLOW,
    createWorkflowPerTargetSite: false,

    startWorkflowFormExtension: StartWorkflowFormExtension<DeeplViewModel>({

      computeViewModel(): DeeplViewModel {
        return { createProject: false };
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

getDeeplDirectTranslationWorkflowPlugin().then((workflowPlugin) => {
  workflowPlugins._.addTranslationWorkflowPlugin(workflowPlugin);
});

const getDeeplDirectTranslationWorkflowLocalization = async (): Promise<WorkflowLocalization> => {
  return {
    displayName: Deepl_properties.TranslationDeeplDirect_displayName,
    description: Deepl_properties.TranslationDeeplDirect_description,
    svgIcon: deeplWorkflowIcon,
  };
};

getDeeplDirectTranslationWorkflowLocalization().then((workflowLocalization) => {
  workflowLocalizationRegistry._.addLocalization(DEEPL_DIRECT_TRANSLATION_WORKFLOW, workflowLocalization);
});

// --- DEEPL WORKFLOW ISSUES LOCALIZATION ---
const getWorkflowIssuesLocalization = async (): Promise<WorkflowIssuesLocalization> => {
  const localize = await getLocalizer(Deepl_properties);
  return {
    "DEEPL-WF-10000": localize("DEEPL-WF-10000_text"),
    "DEEPL-WF-20000": localize("DEEPL-WF-20000_text"),
    "DEEPL-WF-20001": localize("DEEPL-WF-20001_text"),
    "DEEPL-WF-20002": localize("DEEPL-WF-20002_text"),
    "DEEPL-WF-20003": localize("DEEPL-WF-20003_text"),
    "DEEPL-WF-20004": localize("DEEPL-WF-20004_text"),
    "DEEPL-WF-50000": localize("DEEPL-WF-50000_text"),
    failedLanguageValidation: localize("failedLanguageValidation_text"),
    unsupportedSourceLocales: {
      singular: localize("ERROR_singular_text"),
      plural: localize("ERROR_plural_text"),
    },
    unsupportedTargetLocales: {
      singular: localize("WARN_target_singular_text"),
      plural: localize("WARN_target_plural_text"),
    },

    SUCCESS: {
      singular: localize("SUCCESS_singular_text"),
      plural: localize("SUCCESS_plural_text"),
    }
  };
};

getWorkflowIssuesLocalization().then((workflowIssuesLocalization) => {
  workflowLocalizationRegistry._.addIssuesLocalization(workflowIssuesLocalization);
});

// TODO: Not supported by CustomWorkflowAPI yet
// Add validation issue mappings for ui components in workflow dialog (see: WorkflowComponentValidationStateUtil.ts)
/*
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
*/
