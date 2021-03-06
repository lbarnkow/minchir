package com.github.lbarnkow.minchir.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.github.lbarnkow.minchir.App;
import com.github.lbarnkow.minchir.util.SystemExitException;

class AppTest {

  @Test
  void testPicoCliHelp() {
    var e = assertThrows(SystemExitException.class, () -> App.setupCli());
    assertThat(e.getStatus()).isEqualTo(1);
  }
}
