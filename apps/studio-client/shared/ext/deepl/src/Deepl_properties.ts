interface Deepl_properties {
  TranslationDeeplDirect_description: string;
  TranslationDeeplDirect_displayName: string;
  TranslationDeepl_displayName: string,
  TranslationDeepl_description: string,
  TranslationDeepl_field_createProject_label: string;
  TranslationDeepl_field_createProject_tooltip: string;
  TranslationDeepl_state_rollbackTranslation_displayName: string,
  TranslationDeepl_state_finishTranslation_displayName: string,

  SUCCESS_singular_text: string,
  SUCCESS_plural_text: string,
  ERROR_singular_text: string,
  ERROR_plural_text: string,
  WARN_target_singular_text: string;
  WARN_target_plural_text: string;

  "DEEPL-WF-10000_text": string;
  "DEEPL-WF-20000_text": string;
  "DEEPL-WF-20001_text": string;
  "DEEPL-WF-20002_text": string;
  "DEEPL-WF-20003_text": string;
  "DEEPL-WF-20004_text": string;
  "DEEPL-WF-50000_text": string;
  failedLanguageValidation_text: string;
}

const Deepl_properties: Deepl_properties = {
  TranslationDeepl_description: "Reviewed Translation with DeepL",
  TranslationDeepl_displayName: "Reviewed Translation with DeepL",
  TranslationDeeplDirect_displayName: "AI Translation with DeepL",
  TranslationDeeplDirect_description: "AI Translation with DeepL",
  TranslationDeepl_field_createProject_label: "Create Project",
  TranslationDeepl_field_createProject_tooltip: "Create a project with all modified content when workflow is finished",
  TranslationDeepl_state_rollbackTranslation_displayName: "Reject changes",
  TranslationDeepl_state_finishTranslation_displayName: "Finish content Localization",
  SUCCESS_singular_text: "The translation result has successfully been imported.",
  SUCCESS_plural_text: "The translation results have successfully been imported.",
  ERROR_singular_text: "A content item can't be translated because the language is not supported by Deepl.",
  ERROR_plural_text: "Various content items can't be translated because the language is not supported by Deepl.",
  WARN_target_singular_text: "Untick unsupported Site to translate to. See DeepL documentation for more infos.",
  WARN_target_plural_text: "Untick unsupported Sites to translate to. See DeepL documentation for more infos.",

  "DEEPL-WF-10000_text": "An unexpected error occurred.",
  "DEEPL-WF-20000_text": "The configured DeepL API key is invalid.",
  "DEEPL-WF-20001_text": "The DeepL quota was exceeded.",
  "DEEPL-WF-20002_text": "DeepL API was called too many times in a short period of time.",
  "DEEPL-WF-20003_text": "The connection to the DeepL API failed.",
  "DEEPL-WF-20004_text": "The DeepL API endpoint was not found at the configured serverUrl.",
  "DEEPL-WF-50000_text": "The translation processing failed for some content items.",
  failedLanguageValidation_text: "The DeepL language validation failed. Review Studio Server logs for more details."
};

export default Deepl_properties;
