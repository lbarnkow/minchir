package com.github.lbarnkow.minchir.clickmock;

import org.junit.jupiter.api.Test;

import com.github.lbarnkow.minchir.App;
import com.github.lbarnkow.minchir.testutilities.DefaultTestEnvironmentVariables;
import com.github.lbarnkow.minchir.testutilities.baseclasses.ClickMockTest;
import com.github.lbarnkow.minchir.testutilities.ldap.LdapTest;
import com.github.lbarnkow.minchir.testutilities.wiremock.FileBasedWireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.unboundid.ldap.listener.InMemoryDirectoryServer;

@FileBasedWireMock(stubs = {"/hydra_wiremock.yaml"})
@LdapTest(ldifFiles = {"users.ldif"})
@ClickMockTest
public class MockedEnvironment {

  @Test
  void run_mock_until_input_read_from_stdin(WireMockRuntimeInfo wmInfo, InMemoryDirectoryServer ldap) throws Exception {
    var settings = DefaultTestEnvironmentVariables.build2(wmInfo.getHttpBaseUrl(), ldap.getListenPort());

    try (var app = new App().javalinApp(settings)) {
      app.start(8080);

      System.err.println("MOCKED ENVIRONMENT RUNNING.");
      var read = System.in.read();

      if (read == -1) {
        Thread.sleep(Long.MAX_VALUE);
      }
    }
  }
}
