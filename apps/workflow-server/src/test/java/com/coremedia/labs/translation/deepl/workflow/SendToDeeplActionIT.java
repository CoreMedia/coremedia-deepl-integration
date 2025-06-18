package com.coremedia.labs.translation.deepl.workflow;

import com.deepl.api.DeepLClient;
import com.deepl.api.TextResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SendToDeeplActionIT {
  private static final String API_KEY_ENV_VAR = "DEEPL_API_KEY";

  @Test
  @EnabledIfEnvironmentVariable(named = API_KEY_ENV_VAR, matches = ".*")
  public void testTranslate() throws Exception {
    String apiKey = System.getenv(API_KEY_ENV_VAR);
    DeepLClient deepLClient = new DeepLClient(apiKey);
    String expected = "Hallo Welt!";
    String source = "Hello World!";
    TextResult result = deepLClient.translateText(source, "en", "de");
    assertEquals(expected, result.getText());
  }

}
