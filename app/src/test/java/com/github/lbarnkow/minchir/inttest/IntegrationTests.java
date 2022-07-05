package com.github.lbarnkow.minchir.inttest;

import com.github.lbarnkow.minchir.testutilities.baseclasses.IntegrationTest;
import com.github.lbarnkow.minchir.testutilities.web.WebTestsBase;

@IntegrationTest
public class IntegrationTests extends WebTestsBase {

  @Override
  protected String getOryBaseUrl() {
    return "https://localhost:14444";
  }

  @Override
  protected String getOryAdminBaseUrl() {
    return "https://localhost:14445";
  }

  @Override
  protected String getMinchirBaseUrl() {
    return "http://localhost:18080";
  }

  @Override
  protected String getClientId() {
    return "oidc-client-1";
  }

  @Override
  protected String getLoginCallback() {
    return "http://localhost:28080/";
  }

  @Override
  protected String getLogoutCallback() {
    return "http://localhost:28080/";
  }

  // The actual tests are defined in super class WebTestsBase
}
