package com.github.lbarnkow.minchir.testutilities.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

public abstract class WebTestsBase {
  protected abstract String getOryBaseUrl();

  protected abstract String getOryAdminBaseUrl();

  protected abstract String getMinchirBaseUrl();

  protected abstract String getClientId();

  protected abstract String getLoginCallback();

  protected abstract String getLogoutCallback();

  @BeforeEach
  protected void before(TestInfo testInfo) throws Exception {
    OryHydraHelper.cleanConsentSessions(getOryAdminBaseUrl());

    System.err.println(testInfo.getDisplayName());
  }

  @Test
  void shouldLoginAndConsentAndLogout() throws Exception {
    try (final WebClient webClient = WebClientHelper.init()) {
      var page =
          BrowserTestHelper.startAuthorizationCodeFlow(webClient, getOryBaseUrl(), getClientId(), getLoginCallback());
      page = BrowserTestHelper.submitMinchirLoginForm(page, true, "janedoe", "12345", "", true, getMinchirBaseUrl());
      page = BrowserTestHelper.submitMinchirConsentForm(page, true, true, getMinchirBaseUrl());
      BrowserTestHelper.assertSuccessfulLoginFlow(page, getLoginCallback());

      page = BrowserTestHelper.startLogoutFlow(webClient, getOryBaseUrl());
      page = BrowserTestHelper.submitMinchirLogoutForm(page, true, getMinchirBaseUrl());
      BrowserTestHelper.assertSuccessfulLogoutFlow(page, getLogoutCallback());
    }
  }

  @Test
  void shouldCancelLogin() throws Exception {
    try (final WebClient webClient = WebClientHelper.init()) {
      var page =
          BrowserTestHelper.startAuthorizationCodeFlow(webClient, getOryBaseUrl(), getClientId(), getLoginCallback());
      assertThat(page).isInstanceOf(HtmlPage.class);
      var html = (HtmlPage) page;

      page = BrowserTestHelper.submitMinchirLoginForm(html, false, "", "", "", false, getMinchirBaseUrl());
      assertThat(page).isInstanceOf(HtmlPage.class);
      html = (HtmlPage) page;

      BrowserTestHelper.assertCancelledLoginFlow(html, getLoginCallback());
    }
  }

  @Test
  void shouldFailLoginWithWrongCredentials() throws Exception {
    try (final WebClient webClient = WebClientHelper.init()) {
      var page =
          BrowserTestHelper.startAuthorizationCodeFlow(webClient, getOryBaseUrl(), getClientId(), getLoginCallback());
      assertThat(page).isInstanceOf(HtmlPage.class);
      var html = (HtmlPage) page;

      page = BrowserTestHelper.submitMinchirLoginForm(html, true, "janedoe", "wrongpwd", "wrongpwd", true,
          getMinchirBaseUrl());
      assertThat(page).isInstanceOf(HtmlPage.class);
      html = (HtmlPage) page;

      BrowserTestHelper.assertPageIsMinchirLoginForm(html, getMinchirBaseUrl());
      assertThat(html.getWebResponse().getStatusCode()).isEqualTo(401);
      assertThat(html.getWebResponse().getContentAsString()).contains("Login failed; your credentials were incorrect.");
    }
  }

  @Test
  void shouldLoginAndCancelConsent() throws Exception {
    try (final WebClient webClient = WebClientHelper.init()) {
      var page =
          BrowserTestHelper.startAuthorizationCodeFlow(webClient, getOryBaseUrl(), getClientId(), getLoginCallback());
      assertThat(page).isInstanceOf(HtmlPage.class);
      var html = (HtmlPage) page;

      page = BrowserTestHelper.submitMinchirLoginForm(html, true, "janedoe", "12345", "", true, getMinchirBaseUrl());
      assertThat(page).isInstanceOf(HtmlPage.class);
      html = (HtmlPage) page;

      page = BrowserTestHelper.submitMinchirConsentForm(html, false, false, getMinchirBaseUrl());

      BrowserTestHelper.assertCancelledConsentFlow(page, getLoginCallback());
    }
  }

  @Test
  void shouldLoginAndConsentAndCancelLogout() throws Exception {
    try (final WebClient webClient = WebClientHelper.init()) {
      var page =
          BrowserTestHelper.startAuthorizationCodeFlow(webClient, getOryBaseUrl(), getClientId(), getLoginCallback());
      assertThat(page).isInstanceOf(HtmlPage.class);
      var html = (HtmlPage) page;

      page = BrowserTestHelper.submitMinchirLoginForm(html, true, "janedoe", "12345", "", true, getMinchirBaseUrl());
      assertThat(page).isInstanceOf(HtmlPage.class);
      html = (HtmlPage) page;

      page = BrowserTestHelper.submitMinchirConsentForm(html, true, true, getMinchirBaseUrl());

      BrowserTestHelper.assertSuccessfulLoginFlow(page, getLoginCallback());

      page = BrowserTestHelper.startLogoutFlow(webClient, getOryBaseUrl());
      assertThat(page).isInstanceOf(HtmlPage.class);
      html = (HtmlPage) page;

      page = BrowserTestHelper.submitMinchirLogoutForm(html, false, getMinchirBaseUrl());

      // TODO: see LogoutHandler.java
      assertThat(page.getUrl().toExternalForm()).contains("404");
      assertThat(page.getWebResponse().getStatusCode()).isEqualTo(404);
    }
  }

  @Test
  void shouldLoginAndConsentAndRememberLogin() throws Exception {
    try (final WebClient webClient = WebClientHelper.init()) {
      // Login and consent once (check "remember" on both)
      var page =
          BrowserTestHelper.startAuthorizationCodeFlow(webClient, getOryBaseUrl(), getClientId(), getLoginCallback());
      assertThat(page).isInstanceOf(HtmlPage.class);
      var html = (HtmlPage) page;

      page = BrowserTestHelper.submitMinchirLoginForm(html, true, "janedoe", "12345", "", true, getMinchirBaseUrl());
      assertThat(page).isInstanceOf(HtmlPage.class);
      html = (HtmlPage) page;

      page = BrowserTestHelper.submitMinchirConsentForm(html, true, true, getMinchirBaseUrl());

      BrowserTestHelper.assertSuccessfulLoginFlow(page, getLoginCallback());

      // Try to "login" again with valid hydra session cookie
      page =
          BrowserTestHelper.startAuthorizationCodeFlow(webClient, getOryBaseUrl(), getClientId(), getLoginCallback());
      assertThat(page).isInstanceOf(HtmlPage.class);
      html = (HtmlPage) page;

      BrowserTestHelper.assertSuccessfulLoginFlow(page, getLoginCallback());
    }
  }

  @Test
  void shouldLoginAndConsentAndLogoutAndLoginAndRememberConsent() throws Exception {
    try (final WebClient webClient = WebClientHelper.init()) {
      // Login and consent once (check "remember" on both)
      var page =
          BrowserTestHelper.startAuthorizationCodeFlow(webClient, getOryBaseUrl(), getClientId(), getLoginCallback());
      assertThat(page).isInstanceOf(HtmlPage.class);
      var html = (HtmlPage) page;

      page = BrowserTestHelper.submitMinchirLoginForm(html, true, "janedoe", "12345", "", true, getMinchirBaseUrl());
      assertThat(page).isInstanceOf(HtmlPage.class);
      html = (HtmlPage) page;

      page = BrowserTestHelper.submitMinchirConsentForm(html, true, true, getMinchirBaseUrl());

      BrowserTestHelper.assertSuccessfulLoginFlow(page, getLoginCallback());

      page = BrowserTestHelper.startLogoutFlow(webClient, getOryBaseUrl());
      assertThat(page).isInstanceOf(HtmlPage.class);
      html = (HtmlPage) page;

      page = BrowserTestHelper.submitMinchirLogoutForm(html, true, getMinchirBaseUrl());

      BrowserTestHelper.assertSuccessfulLogoutFlow(page, getLogoutCallback());

      // Login again, consent should be skipped
      page =
          BrowserTestHelper.startAuthorizationCodeFlow(webClient, getOryBaseUrl(), getClientId(), getLoginCallback());
      assertThat(page).isInstanceOf(HtmlPage.class);
      html = (HtmlPage) page;

      page = BrowserTestHelper.submitMinchirLoginForm(html, true, "janedoe", "12345", "", true, getMinchirBaseUrl());
      assertThat(page).isInstanceOf(HtmlPage.class);
      html = (HtmlPage) page;

      BrowserTestHelper.assertSuccessfulLoginFlow(page, getLoginCallback());
    }
  }

  @Test
  void shouldFailWithRandomLoginChallenge() throws Exception {
    try (final WebClient webClient = WebClientHelper.init()) {
      var page = webClient.getPage(getMinchirBaseUrl() + "/login?login_challenge=notok");
      assertThat(page).isInstanceOf(HtmlPage.class);
      var html = (HtmlPage) page;

      assertThat(html.getWebResponse().getStatusCode()).isEqualTo(400);
    }
  }

  @Test
  void shouldFailWithRandomConsentChallenge() throws Exception {
    try (final WebClient webClient = WebClientHelper.init()) {
      var page = webClient.getPage(getMinchirBaseUrl() + "/consent?consent_challenge=notok");
      assertThat(page).isInstanceOf(HtmlPage.class);
      var html = (HtmlPage) page;

      assertThat(html.getWebResponse().getStatusCode()).isEqualTo(400);
    }
  }

  @Test
  void shouldFailWithRandomLogoutChallenge() throws Exception {
    try (final WebClient webClient = WebClientHelper.init()) {
      var page = webClient.getPage(getMinchirBaseUrl() + "/logout?logout_challenge=notok");
      assertThat(page).isInstanceOf(HtmlPage.class);
      var html = (HtmlPage) page;

      assertThat(html.getWebResponse().getStatusCode()).isEqualTo(400);
    }
  }

  @Test
  void shouldFailWithMissingMandatoryQueryParam() throws Exception {
    try (final WebClient webClient = WebClientHelper.init()) {
      var page = webClient.getPage(getMinchirBaseUrl() + "/login?login_challenge_is_missing=notok");
      assertThat(page).isInstanceOf(HtmlPage.class);
      var html = (HtmlPage) page;

      assertThat(html.getWebResponse().getStatusCode()).isEqualTo(400);
    }
  }

  @Test
  void shouldFailWithBadCsrfTokens() throws Exception {
    try (final WebClient webClient = WebClientHelper.init()) {
      var page =
          BrowserTestHelper.startAuthorizationCodeFlow(webClient, getOryBaseUrl(), getClientId(), getLoginCallback());
      assertThat(page).isInstanceOf(HtmlPage.class);
      var html = (HtmlPage) page;

      var csrfField = html.getForms().get(0).getInputByName("csrf_token");
      csrfField.setValue("1111" + csrfField.getValue().substring(4));

      page = BrowserTestHelper.submitMinchirLoginForm(html, true, "janedoe", "12345", "", true, getMinchirBaseUrl());
      assertThat(page).isInstanceOf(HtmlPage.class);
      html = (HtmlPage) page;

      assertThat(html.getWebResponse().getStatusCode()).isEqualTo(400);
    }
  }
}
