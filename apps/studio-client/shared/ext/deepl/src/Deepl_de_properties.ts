import ResourceBundleUtil from "@jangaroo/runtime/l10n/ResourceBundleUtil";
import Deepl_properties from "./Deepl_properties";

ResourceBundleUtil.override(Deepl_properties, {
  TranslationDeepl_description: "Übersetzung mit DeepL",
  TranslationDeepl_displayName: "Übersetzung mit DeepL",
  TranslationDeepl_field_createProject_label: "Projekt erstellen",
  TranslationDeepl_field_createProject_tooltip: "Erstellt ein Projekt, mit allen geänderten Inhalten, wenn der Workflow beendet ist",
  SUCCESS_singular_text: "Das Übersetzungsergebnis wurde erfolgreich importiert.",
  SUCCESS_plural_text: "Die Übersetzungsergebnisse wurden erfolgreich importiert.",
  ERROR_singular_text: "Ein Contentitem kann nicht übersetzt werden, weil die Sprache von Deepl nicht unterstützt wird.",
  ERROR_plural_text: "Verschiedene Contentitems können nicht übersetzt werden, weil die Sprache von Deepl nicht unterstützt wird.",
  ERROR_target_singular_text: "Deaktivieren Sie das Kontrollkästchen für die nicht unterstützte Übersetzung. Siehe DeepL Dokumentation für weitere Informationen.",
  ERROR_target_plural_text:"Deaktivieren Sie die Kontrollkästchen für die nicht unterstützten Übersetzungen. Siehe DeepL Dokumentation für weitere Informationen.",
});
