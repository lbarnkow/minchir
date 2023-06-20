package com.github.lbarnkow.minchir.test.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.github.lbarnkow.minchir.config.Config;
import com.github.lbarnkow.minchir.test.testutilities.Resource;

public class ConfigTest {

  @Test
  void testConfigOverlays() {
    var config = Config.load( //
        Resource.resourcePath("config/config1.yaml"), //
        Resource.resourcePath("config/config2.yaml"), //
        Resource.resourcePath("config/config3.yaml") //
    );

    assertThat(config.getServer().getAssetsPath()).isEqualTo("config3");
    assertThat(config.getServer().getPort()).isEqualTo(18080);
    assertThat(config.getCsrf().getTotpTtlSeconds()).isEqualTo(300);
    assertThat(config.getCsrf().getTotpKey()).isEqualTo("notnull");
    assertThat(config.getCsrf().getHmacKey()).isEqualTo("notnull");
    assertThat(config.getHydra().getTimeoutMilliseconds()).isEqualTo(5001);
    assertThat(config.getLdap().getBindPassword()).isEqualTo("password");
    assertThat(config.getLdap().getUserAttributeMail()).isEqualTo("email");
    assertThat(config.getLdap().getUserAttributeSurname()).isEqualTo("sn");
  }
}
