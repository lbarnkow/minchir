package com.github.lbarnkow.minchir.test.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.github.lbarnkow.minchir.config.Scopes;
import com.github.lbarnkow.minchir.test.testutilities.Resource;

public class ScopesTest {

  @Test
  void testConfigOverlays() {
    var scopes = Scopes.load( //
        Resource.resourcePath("scopes/scopes1.yaml"), //
        Resource.resourcePath("scopes/scopes2.yaml"), //
        Resource.resourcePath("scopes/scopes3.yaml") //
    );

    assertThat(scopes.getClaims("scopes1")).containsOnly("three", "four", "five");
    assertThat(scopes.getClaims("scopes2")).containsOnly("one", "two", "three");
    assertThat(scopes.getClaims("scopes3")).containsOnly("one", "two", "three");
  }
}
