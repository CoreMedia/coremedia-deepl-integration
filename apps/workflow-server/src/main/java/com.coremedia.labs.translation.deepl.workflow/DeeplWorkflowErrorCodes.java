package com.coremedia.labs.translation.deepl.workflow;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;

@DefaultAnnotation(NonNull.class)
final class DeeplWorkflowErrorCodes {

  // ==== 10###: General/Unknown Problems
  static final String UNKNOWN_ERROR = "DEEPL-WF-10000";

  // ==== 20###: DeepL Call Problems
  static final String AUTHORIZATION_ERROR = "DEEPL-WF-20000";
  static final String QUOTA_EXCEEDED_ERROR = "DEEPL-WF-20001";
  static final String TOO_MANY_REQUESTS_ERROR = "DEEPL-WF-20002";
  static final String CONNECTION_ERROR = "DEEPL-WF-20003";
  static final String NOT_FOUND_ERROR = "DEEPL-WF-20004";

  // ==== 50###: Translation/XLIFF Problems
  static final String ITEM_TRANSLATION_FAILURE = "DEEPL-WF-50000";

  private DeeplWorkflowErrorCodes() {
  }
}
