package com.github.lbarnkow.minchir.handlers.request;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

import com.github.lbarnkow.minchir.App;
import com.github.lbarnkow.minchir.config.Settings;
import com.github.lbarnkow.minchir.testutilities.DefaultTestEnvironmentVariables;
import com.github.lbarnkow.minchir.testutilities.baseclasses.BaseTest;
import com.github.lbarnkow.minchir.testutilities.ldap.LdapTest;
import com.github.lbarnkow.minchir.testutilities.web.WebTestsBase;
import com.github.lbarnkow.minchir.testutilities.wiremock.FileBasedWireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.unboundid.ldap.listener.InMemoryDirectoryServer;

import io.javalin.Javalin;

@BaseTest
@FileBasedWireMock(stubs = {"/hydra_wiremock2.yaml"})
@LdapTest(ldifFiles = {"users.ldif"})
public class HandlerBrowserTests extends WebTestsBase {

  private Settings settings;
  private Javalin app;

  @Override
  protected void before(TestInfo testInfo) throws Exception {}

  @BeforeEach
  void setupEnvironmentVars( //
      TestInfo testInfo, WireMockRuntimeInfo wmInfo, InMemoryDirectoryServer ldap) throws Exception {

    settings = DefaultTestEnvironmentVariables.build2(wmInfo.getHttpBaseUrl(), ldap.getListenPort());
    super.before(testInfo);
    app = new App().javalinApp(settings);
    app.start();
  }

  @AfterEach
  void shutdownJavalin() {
    app.close();
  }

  @Override
  protected String getOryBaseUrl() {
    return settings.getConfig().getHydra().getAdminUrl();
  }

  @Override
  protected String getOryAdminBaseUrl() {
    return getOryBaseUrl();
  }

  @Override
  protected String getMinchirBaseUrl() {
    return "http://localhost:" + settings.getConfig().getServer().getPort();
  }

  @Override
  protected String getClientId() {
    return "oidc-client-1";
  }

  @Override
  protected String getLoginCallback() {
    return "http://localhost:" + settings.getConfig().getServer().getPort() + "/";
  }

  @Override
  protected String getLogoutCallback() {
    return "http://localhost:" + settings.getConfig().getServer().getPort() + "/";
  }
}
