package com.github.lbarnkow.minchir.test.inttest;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.github.lbarnkow.minchir.test.testutilities.baseclasses.IntegrationTest;

@IntegrationTest
public class IntegrationTests {

  @BeforeEach
  void before() throws Exception {
    OryHydraHelper.cleanConsentSessions();
  }

  @Test
  void shouldLoginAndConsentAndLogout() throws Exception {
    try (final WebClient webClient = WebClientHelper.init()) {
      var page = FlowHelper.startAuthorizationCodeFlow(webClient);
      assertThat(page).isInstanceOf(HtmlPage.class);
      var html = (HtmlPage) page;

      page = FlowHelper.submitMinchirLoginForm(html, true, "janedoe", "12345", "", true);
      assertThat(page).isInstanceOf(HtmlPage.class);
      html = (HtmlPage) page;

      page = FlowHelper.submitMinchirConsentForm(html, true, true);

      FlowHelper.assertSuccessfulLoginFlow(page);

      page = FlowHelper.startLogoutFlow(webClient);
      assertThat(page).isInstanceOf(HtmlPage.class);
      html = (HtmlPage) page;

      page = FlowHelper.submitMinchirLogoutForm(html, true);

      FlowHelper.assertSuccessfulLogoutFlow(page);
    }
  }

  @Test
  void shouldCancelLogin() throws Exception {
    try (final WebClient webClient = WebClientHelper.init()) {
      var page = FlowHelper.startAuthorizationCodeFlow(webClient);
      assertThat(page).isInstanceOf(HtmlPage.class);
      var html = (HtmlPage) page;

      page = FlowHelper.submitMinchirLoginForm(html, false, "", "", "", false);
      assertThat(page).isInstanceOf(HtmlPage.class);
      html = (HtmlPage) page;

      FlowHelper.assertCancelledLoginFlow(html);
    }
  }

  @Test
  void shouldFailLoginWithWrongCredentials() throws Exception {
    try (final WebClient webClient = WebClientHelper.init()) {
      var page = FlowHelper.startAuthorizationCodeFlow(webClient);
      assertThat(page).isInstanceOf(HtmlPage.class);
      var html = (HtmlPage) page;

      page = FlowHelper.submitMinchirLoginForm(html, true, "janedoe", "wrongpwd", "wrongpwd", true);
      assertThat(page).isInstanceOf(HtmlPage.class);
      html = (HtmlPage) page;

      FlowHelper.assertPageIsMinchirLoginForm(html);
      assertThat(html.getWebResponse().getStatusCode()).isEqualTo(401);
      assertThat(html.getWebResponse().getContentAsString()).contains("Login failed; your credentials were incorrect.");
    }
  }

  @Test
  void shouldLoginAndCancelConsent() throws Exception {
    try (final WebClient webClient = WebClientHelper.init()) {
      var page = FlowHelper.startAuthorizationCodeFlow(webClient);
      assertThat(page).isInstanceOf(HtmlPage.class);
      var html = (HtmlPage) page;

      page = FlowHelper.submitMinchirLoginForm(html, true, "janedoe", "12345", "", true);
      assertThat(page).isInstanceOf(HtmlPage.class);
      html = (HtmlPage) page;

      page = FlowHelper.submitMinchirConsentForm(html, false, false);

      FlowHelper.assertCancelledConsentFlow(page);
    }
  }

  @Test
  void shouldLoginAndConsentAndCancelLogout() throws Exception {
    try (final WebClient webClient = WebClientHelper.init()) {
      var page = FlowHelper.startAuthorizationCodeFlow(webClient);
      assertThat(page).isInstanceOf(HtmlPage.class);
      var html = (HtmlPage) page;

      page = FlowHelper.submitMinchirLoginForm(html, true, "janedoe", "12345", "", true);
      assertThat(page).isInstanceOf(HtmlPage.class);
      html = (HtmlPage) page;

      page = FlowHelper.submitMinchirConsentForm(html, true, true);

      FlowHelper.assertSuccessfulLoginFlow(page);

      page = FlowHelper.startLogoutFlow(webClient);
      assertThat(page).isInstanceOf(HtmlPage.class);
      html = (HtmlPage) page;

      page = FlowHelper.submitMinchirLogoutForm(html, false);

      // TODO: see LogoutHandler.java
      assertThat(page.getUrl().toExternalForm()).contains("404");
      assertThat(page.getWebResponse().getStatusCode()).isEqualTo(404);
    }
  }

  @Test
  void shouldLoginAndConsentAndRememberLogin() throws Exception {
    try (final WebClient webClient = WebClientHelper.init()) {
      // Login and consent once (check "remember" on both)
      var page = FlowHelper.startAuthorizationCodeFlow(webClient);
      assertThat(page).isInstanceOf(HtmlPage.class);
      var html = (HtmlPage) page;

      page = FlowHelper.submitMinchirLoginForm(html, true, "janedoe", "12345", "", true);
      assertThat(page).isInstanceOf(HtmlPage.class);
      html = (HtmlPage) page;

      page = FlowHelper.submitMinchirConsentForm(html, true, true);

      FlowHelper.assertSuccessfulLoginFlow(page);

      // Try to "login" again with valid hydra session cookie
      page = FlowHelper.startAuthorizationCodeFlow(webClient);
      assertThat(page).isInstanceOf(HtmlPage.class);
      html = (HtmlPage) page;

      FlowHelper.assertSuccessfulLoginFlow(page);
    }
  }

  @Test
  void shouldLoginAndConsentAndLogoutAndLoginAndRememberConsent() throws Exception {
    try (final WebClient webClient = WebClientHelper.init()) {
      // Login and consent once (check "remember" on both)
      var page = FlowHelper.startAuthorizationCodeFlow(webClient);
      assertThat(page).isInstanceOf(HtmlPage.class);
      var html = (HtmlPage) page;

      page = FlowHelper.submitMinchirLoginForm(html, true, "janedoe", "12345", "", true);
      assertThat(page).isInstanceOf(HtmlPage.class);
      html = (HtmlPage) page;

      page = FlowHelper.submitMinchirConsentForm(html, true, true);

      FlowHelper.assertSuccessfulLoginFlow(page);

      page = FlowHelper.startLogoutFlow(webClient);
      assertThat(page).isInstanceOf(HtmlPage.class);
      html = (HtmlPage) page;

      page = FlowHelper.submitMinchirLogoutForm(html, true);

      FlowHelper.assertSuccessfulLogoutFlow(page);

      // Login again, consent should be skipped
      page = FlowHelper.startAuthorizationCodeFlow(webClient);
      assertThat(page).isInstanceOf(HtmlPage.class);
      html = (HtmlPage) page;

      page = FlowHelper.submitMinchirLoginForm(html, true, "janedoe", "12345", "", true);
      assertThat(page).isInstanceOf(HtmlPage.class);
      html = (HtmlPage) page;

      FlowHelper.assertSuccessfulLoginFlow(page);
    }
  }

  @Test
  void shouldFailWithRandomLoginChallenge() throws Exception {
    try (final WebClient webClient = WebClientHelper.init()) {
      var page = webClient.getPage(FlowHelper.MINCHIR_BASE_URL + "/login?login_challenge=notok");
      assertThat(page).isInstanceOf(HtmlPage.class);
      var html = (HtmlPage) page;

      assertThat(html.getWebResponse().getStatusCode()).isEqualTo(400);
    }
  }

  @Test
  void shouldFailWithRandomConsentChallenge() throws Exception {
    try (final WebClient webClient = WebClientHelper.init()) {
      var page = webClient.getPage(FlowHelper.MINCHIR_BASE_URL + "/consent?consent_challenge=notok");
      assertThat(page).isInstanceOf(HtmlPage.class);
      var html = (HtmlPage) page;

      assertThat(html.getWebResponse().getStatusCode()).isEqualTo(400);
    }
  }

  @Test
  void shouldFailWithRandomLogoutChallenge() throws Exception {
    try (final WebClient webClient = WebClientHelper.init()) {
      var page = webClient.getPage(FlowHelper.MINCHIR_BASE_URL + "/logout?logout_challenge=notok");
      assertThat(page).isInstanceOf(HtmlPage.class);
      var html = (HtmlPage) page;

      assertThat(html.getWebResponse().getStatusCode()).isEqualTo(400);
    }
  }

  @Test
  void shouldFailWithMissingMandatoryQueryParam() throws Exception {
    try (final WebClient webClient = WebClientHelper.init()) {
      var page = webClient.getPage(FlowHelper.MINCHIR_BASE_URL + "/login?login_challenge_is_missing=notok");
      assertThat(page).isInstanceOf(HtmlPage.class);
      var html = (HtmlPage) page;

      assertThat(html.getWebResponse().getStatusCode()).isEqualTo(400);
    }
  }

  @Test
  void shouldFailWithBadCsrfTokens() throws Exception {
    try (final WebClient webClient = WebClientHelper.init()) {
      var page = FlowHelper.startAuthorizationCodeFlow(webClient);
      assertThat(page).isInstanceOf(HtmlPage.class);
      var html = (HtmlPage) page;

      var csrfField = html.getForms().get(0).getInputByName("csrf_token");
      csrfField.setValue("1111" + csrfField.getValue().substring(4));

      page = FlowHelper.submitMinchirLoginForm(html, true, "janedoe", "12345", "", true);
      assertThat(page).isInstanceOf(HtmlPage.class);
      html = (HtmlPage) page;

      assertThat(html.getWebResponse().getStatusCode()).isEqualTo(400);
    }
  }
}
