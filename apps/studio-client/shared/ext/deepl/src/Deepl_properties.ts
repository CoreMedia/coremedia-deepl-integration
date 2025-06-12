interface Deepl_properties {
  TranslationDeepl_displayName: string,
  TranslationDeepl_description: string,
  TranslationDeeplDirect_displayName: string,
  TranslationDeeplDirect_description: string,
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
}

const Deepl_properties : Deepl_properties = {
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
  ERROR_singular_text: "A Contentitem can't be translated because the language is not supported by Deepl.",
  ERROR_plural_text: "Various Contentitems can't be translated because the language is not supported by Deepl.",
  WARN_target_singular_text: "Untick unsupported Site to translate to. See DeepL Documentation for more infos.",
  WARN_target_plural_text:"Untick unsupported Sites to translate to. See DeepL Documentation for more infos.",
}

export default Deepl_properties;
