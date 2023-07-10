import ResourceBundleUtil from "@jangaroo/runtime/l10n/ResourceBundleUtil";
import Deepl_properties from "./Deepl_properties";

ResourceBundleUtil.override(Deepl_properties, {
  TranslationDeepl_description: "DeepLによる翻訳",
  TranslationDeepl_displayName: "DeepLによる翻訳",

  SUCCESS_singular_text: "翻訳結果は正常にインポートされました。",
  SUCCESS_plural_text: "翻訳結果が正常にインポートされました。",
});
