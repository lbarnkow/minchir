package com.github.lbarnkow.minchir.test.inttest;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

public class FlowHelper {

  public static final String MINCHIR_BASE_URL = "http://localhost:18080";

  public static final String ORY_CLIENT_ID = "oidc-client-1";
  public static final String SCOPE = "openid";
  public static final String LOGIN_CALLBACK = "http://localhost:28080/";
  private static final String LOGOUT_CALLBACK = "http://localhost:28080/";
  public static final String STATE = UUID.randomUUID().toString();
  public static final String RESPONSE_TYPE = "code";

  public static Page startAuthorizationCodeFlow(WebClient client) throws Exception {
    var url = OryHydraHelper.getAuthorizationCodeFlowUrl( //
        ORY_CLIENT_ID, RESPONSE_TYPE, SCOPE, LOGIN_CALLBACK, STATE);
    return client.getPage(url);
  }

  public static Page startLogoutFlow(WebClient client) throws Exception {
    var url = OryHydraHelper.getLogoutUrl();
    return client.getPage(url);
  }

  public static Page submitMinchirLoginForm( //
      HtmlPage page, boolean submit, //
      String username, String password, String totp, boolean rememberMe) throws Exception {

    assertPageIsMinchirLoginForm(page);

    var form = page.getForms().get(0);

    if (username != null && !username.isEmpty())
      form.getInputByName("username").type(username);
    if (password != null && !password.isEmpty())
      form.getInputByName("password").type(password);
    if (totp != null && !totp.isEmpty())
      form.getInputByName("totp").type(totp);
    form.getInputByName("rememberme").setChecked(true);

    if (submit)
      return form.getButtonByName("login").click();
    else
      return form.getButtonByName("cancel").click();
  }

  public static Page submitMinchirConsentForm( //
      HtmlPage page, boolean submit, boolean rememberMe) throws Exception {

    assertPageIsMinchirConsentForm(page);;

    var form = page.getForms().get(0);
    form.getInputByName("rememberconsent").setChecked(true);

    if (submit)
      return form.getButtonByName("consent").click();
    else
      return form.getButtonByName("cancel").click();
  }

  public static Page submitMinchirLogoutForm(HtmlPage page, boolean submit) throws Exception {
    assertPageIsMinchirLogoutForm(page);

    var form = page.getForms().get(0);

    if (submit)
      return form.getButtonByName("logout").click();
    else
      return form.getButtonByName("cancel").click();
  }

  public static void assertPageIsMinchirLoginForm(HtmlPage page) {
    assertThat(page.getUrl().toString()).startsWith(MINCHIR_BASE_URL + "/login");
    assertThat(page.getTitleText()).isEqualTo("SSO - Login");

    if (page.getWebResponse().getWebRequest().getHttpMethod().equals(HttpMethod.GET)) {
      var challenge = page.getUrl().getQuery().split("=")[1];
      var form = page.getForms().get(0);
      var hiddenChallenge = form.getInputByName("login_challenge").getValue();
      assertThat(hiddenChallenge).isEqualTo(challenge);
    }
  }

  public static void assertPageIsMinchirConsentForm(HtmlPage page) {
    assertThat(page.getUrl().toString()).startsWith("http://localhost:18080/consent");
    var challenge = page.getUrl().getQuery().split("=")[1];

    assertThat(page.getTitleText()).isEqualTo("SSO - Consent");

    var form = page.getForms().get(0);
    assertThat(form.getInputByName("consent_challenge").getValue()).isEqualTo(challenge);
  }

  public static void assertPageIsMinchirLogoutForm(HtmlPage page) {
    assertThat(page.getUrl().toString()).startsWith("http://localhost:18080/logout");
    var challenge = page.getUrl().getQuery().split("=")[1];

    assertThat(page.getTitleText()).isEqualTo("SSO - Logout");

    var form = page.getForms().get(0);
    assertThat(form.getInputByName("logout_challenge").getValue()).isEqualTo(challenge);
  }

  public static void assertSuccessfulLoginFlow(Page page) {
    assertThat(page.getUrl().toString()).startsWith(LOGIN_CALLBACK);
    assertThat(page.getUrl().getQuery()).contains("scope=" + SCOPE);
    assertThat(page.getUrl().getQuery()).contains("state=" + STATE);
    assertThat(page.getUrl().getQuery()).contains("code=");
  }

  public static void assertCancelledLoginFlow(Page page) {
    assertThat(page.getUrl().toString()).startsWith(LOGIN_CALLBACK);
    assertThat(page.getUrl().getQuery()).contains("error=" + "access_denied");
    assertThat(page.getUrl().getQuery()).contains("state=" + STATE);
  }

  public static void assertCancelledConsentFlow(Page page) {
    assertThat(page.getUrl().toString()).startsWith(LOGIN_CALLBACK);
    assertThat(page.getUrl().getQuery()).contains("error=" + "access_denied");
    assertThat(page.getUrl().getQuery()).contains("state=" + STATE);
  }

  public static void assertSuccessfulLogoutFlow(Page page) {
    assertThat(page.getUrl().toString().startsWith(LOGOUT_CALLBACK));
    assertThat(page.getUrl().getQuery()).isNullOrEmpty();
  }
}
