package com.github.lbarnkow.minchir.testutilities.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

public class BrowserTestHelper {

  public static final String SCOPE = "openid";
  public static final String STATE = "f69ebbf6-c43b-4e3a-b7af-63a4a9dc09ea";
  public static final String RESPONSE_TYPE = "code";

  public static HtmlPage startAuthorizationCodeFlow( //
      WebClient client, String oryBaseUrl, String clientId, String loginCallback) throws Exception {

    var url = OryHydraHelper.getAuthorizationCodeFlowUrl( //
        oryBaseUrl, clientId, RESPONSE_TYPE, SCOPE, loginCallback, STATE);
    return client.getPage(url);
  }

  public static HtmlPage startLogoutFlow(WebClient client, String oryBaseUrl) throws Exception {
    var url = OryHydraHelper.getLogoutUrl(oryBaseUrl);
    return client.getPage(url);
  }

  public static HtmlPage submitMinchirLoginForm( //
      HtmlPage page, boolean submit, //
      String username, String password, String totp, boolean rememberMe, //
      String minchirBaseUrl) throws Exception {

    assertPageIsMinchirLoginForm(page, minchirBaseUrl);

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

  public static HtmlPage submitMinchirConsentForm( //
      HtmlPage page, boolean submit, boolean rememberMe, String minchirBaseUrl) throws Exception {

    assertPageIsMinchirConsentForm(page, minchirBaseUrl);;

    var form = page.getForms().get(0);
    form.getInputByName("rememberconsent").setChecked(true);

    if (submit)
      return form.getButtonByName("consent").click();
    else
      return form.getButtonByName("cancel").click();
  }

  public static HtmlPage submitMinchirLogoutForm(HtmlPage page, boolean submit, String minchirBaseUrl)
      throws Exception {
    assertPageIsMinchirLogoutForm(page, minchirBaseUrl);

    var form = page.getForms().get(0);

    if (submit)
      return form.getButtonByName("logout").click();
    else
      return form.getButtonByName("cancel").click();
  }

  public static void assertPageIsMinchirLoginForm(HtmlPage page, String minchirBaseUrl) {
    assertThat(page.getUrl().toString()).startsWith(minchirBaseUrl + "/login");
    assertThat(page.getTitleText()).isEqualTo("SSO - Login");

    if (page.getWebResponse().getWebRequest().getHttpMethod().equals(HttpMethod.GET)) {
      var challenge = page.getUrl().getQuery().split("=")[1];
      var form = page.getForms().get(0);
      var hiddenChallenge = form.getInputByName("login_challenge").getValue();
      assertThat(hiddenChallenge).isEqualTo(challenge);
    }
  }

  public static void assertPageIsMinchirConsentForm(HtmlPage page, String minchirBaseUrl) {
    assertThat(page.getUrl().toString()).startsWith(minchirBaseUrl + "/consent");
    var challenge = page.getUrl().getQuery().split("=")[1];

    assertThat(page.getTitleText()).isEqualTo("SSO - Consent");

    var form = page.getForms().get(0);
    assertThat(form.getInputByName("consent_challenge").getValue()).isEqualTo(challenge);
  }

  public static void assertPageIsMinchirLogoutForm(HtmlPage page, String minchirBaseUrl) {
    assertThat(page.getUrl().toString()).startsWith(minchirBaseUrl + "/logout");
    var challenge = page.getUrl().getQuery().split("=")[1];

    assertThat(page.getTitleText()).isEqualTo("SSO - Logout");

    var form = page.getForms().get(0);
    assertThat(form.getInputByName("logout_challenge").getValue()).isEqualTo(challenge);
  }

  public static void assertSuccessfulLoginFlow(Page page, String loginCallback) {
    assertThat(page.getUrl().toString()).startsWith(loginCallback);
    assertThat(page.getUrl().getQuery()).contains("scope=" + SCOPE);
    assertThat(page.getUrl().getQuery()).contains("state=" + STATE);
    assertThat(page.getUrl().getQuery()).contains("code=");
  }

  public static void assertCancelledLoginFlow(Page page, String loginCallback) {
    assertThat(page.getUrl().toString()).startsWith(loginCallback);
    assertThat(page.getUrl().getQuery()).contains("error=" + "access_denied");
    assertThat(page.getUrl().getQuery()).contains("state=" + STATE);
  }

  public static void assertCancelledConsentFlow(Page page, String loginCallback) {
    assertThat(page.getUrl().toString()).startsWith(loginCallback);
    assertThat(page.getUrl().getQuery()).contains("error=" + "access_denied");
    assertThat(page.getUrl().getQuery()).contains("state=" + STATE);
  }

  public static void assertSuccessfulLogoutFlow(Page page, String logoutCallback) {
    assertThat(page.getUrl().toString().startsWith(logoutCallback));
    assertThat(page.getUrl().getQuery()).isNullOrEmpty();
  }
}
