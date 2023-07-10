package com.coremedia.labs.translation.deepl.workflow;

import com.deepl.api.TextResult;
import com.deepl.api.Translator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
public class SendToDeeplActionIT {

  private Translator translator;

  @BeforeEach
  public void setUp() {
    String apiKey = System.getenv("deepl.apiKey");
    translator = new Translator(apiKey);
  }

  @Test
  public void testTranslate() throws Exception {
    String expected = "Hallo Welt!";
    String source = "Hello World!";

    TextResult result = translator.translateText(source, "en", "de");
    assertEquals(expected, result.getText());
  }

}
