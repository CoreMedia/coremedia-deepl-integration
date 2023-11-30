import ResourceBundleUtil from "@jangaroo/runtime/l10n/ResourceBundleUtil";
import Deepl_properties from "./Deepl_properties";

ResourceBundleUtil.override(Deepl_properties, {
  TranslationDeepl_description: "DeepLによる翻訳",
  TranslationDeepl_displayName: "DeepLによる翻訳",

  SUCCESS_singular_text: "翻訳結果は正常にインポートされました。",
  SUCCESS_plural_text: "翻訳結果が正常にインポートされました。",
  ERROR_singular_text: "その言語は Deepl でサポートされていないため、Contentitem を翻訳できません。",
  ERROR_plural_text: "Deepl でサポートされていない言語のため、さまざまなコンテンツ項目を翻訳できません。",
  ERROR_target_singular_text: "翻訳するサポートされていないサイトのチェックを外します。詳細は、\"DeepL ドキュメント\" を参照してください",
  ERROR_target_plural_text:"サポートされていない翻訳のチェックボックスをオフにします。詳細は、DeepL のドキュメントを参照してください。",
});
