package com.github.lbarnkow.minchir.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.github.lbarnkow.minchir.config.Translations;
import com.github.lbarnkow.minchir.testutilities.Resource;
import com.github.lbarnkow.minchir.testutilities.baseclasses.BaseTest;

@BaseTest
public class TranslationsTest {

  @Test
  void testTranslationOverlays() {
    var translations = Translations.load( //
        Resource.resourcePath("i18n/translations1.yaml"), //
        Resource.resourcePath("i18n/translations2.yaml"), //
        Resource.resourcePath("i18n/translations3.yaml") //
    );

    assertThat(translations.get("en").get("glob1")).isEqualTo("translations1");
    assertThat(translations.get("en").get("glob2")).isEqualTo("translations2");
    assertThat(translations.get("de").get("glob1")).isEqualTo("translations1");
    assertThat(translations.get("de").get("glob2")).isEqualTo("translations2");
    assertThat(translations.get("fr").get("glob1")).isEqualTo("translations1");
    assertThat(translations.get("fr").get("glob2")).isEqualTo("translations2");
    assertThat(translations.get("pl").get("glob1")).isEqualTo("translations1");
    assertThat(translations.get("pl").get("glob2")).isEqualTo("translations2");

    assertThat(translations.get("en").get("one")).isEqualTo("translations1");
    assertThat(translations.get("en").get("two")).isEqualTo("translations2");
    assertThat(translations.get("en").get("three")).isEqualTo("translations1");
    assertThat(translations.get("en").get("four")).isEqualTo("translations2");
    assertThat(translations.get("de").get("eins")).isEqualTo("translations1");
    assertThat(translations.get("de").get("zwei")).isEqualTo("translations3");
    assertThat(translations.get("fr").get("un")).isEqualTo("translations2");
    assertThat(translations.get("fr").get("deux")).isEqualTo("translations3");
    assertThat(translations.get("pl").get("jeden")).isEqualTo("translations3");

    assertThat(translations.get("invalid").get("glob1")).isEqualTo("translations1");
    assertThat(translations.get("invalid").get("glob2")).isEqualTo("translations2");
    assertThat(translations.get("invalid").get("one")).isEqualTo("translations1");
    assertThat(translations.get("invalid").get("two")).isEqualTo("translations2");
    assertThat(translations.get("invalid").get("four")).isEqualTo("translations2");
  }
}
