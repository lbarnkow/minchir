package com.github.lbarnkow.minchir.test.testutilities;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import com.github.lbarnkow.minchir.App;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.unboundid.ldap.listener.InMemoryDirectoryServer;

@FileBasedWireMock(stubs = {"/hydra_wiremock.yaml"})
@LdapTest(ldifFiles = {"users.ldif"})
public class MockedEnvironment {

  @Test
  @EnabledIfEnvironmentVariable(named = "MOCKED_ENVIRONMENT", matches = "ON")
  void test_successful_non_skipped_login_flow(WireMockRuntimeInfo wmInfo, InMemoryDirectoryServer ldap)
      throws Exception {
    var settings = DefaultTestEnvironmentVariables.build2(wmInfo.getHttpBaseUrl(), ldap.getListenPort());

    try (var app = new App().javalinApp(settings)) {
      app.start(8080);
      System.err.println("MOCKED ENVIRONMENT RUNNING. HIT ENTER TO SHUT DOWN!");
      System.in.read();
    }
  }

}
