interface Deepl_properties {
  TranslationDeepl_displayName: string,
  TranslationDeepl_description: string,
  TranslationDeepl_state_rollbackTranslation_displayName: string,
  TranslationDeepl_state_finishTranslation_displayName: string,

  SUCCESS_singular_text: string,
  SUCCESS_plural_text: string,
}

const Deepl_properties : Deepl_properties = {
  TranslationDeepl_description: "Translation with DeepL",
  TranslationDeepl_displayName: "Translation with DeepL",
  TranslationDeepl_state_rollbackTranslation_displayName: "Reject changes",
  TranslationDeepl_state_finishTranslation_displayName: "Finish content Localization",
  SUCCESS_singular_text: "The translation result has successfully been imported.",
  SUCCESS_plural_text: "The translation results have successfully been imported.",
}

export default Deepl_properties;
