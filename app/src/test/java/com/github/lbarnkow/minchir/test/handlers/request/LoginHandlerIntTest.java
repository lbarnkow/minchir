package com.github.lbarnkow.minchir.test.handlers.request;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jetty.http.HttpStatus.MOVED_TEMPORARILY_302;
import static org.eclipse.jetty.http.HttpStatus.UNAUTHORIZED_401;

import java.io.IOException;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.lbarnkow.minchir.App;
import com.github.lbarnkow.minchir.config.Settings;
import com.github.lbarnkow.minchir.handlers.before.csrf.CSRFSupplier.CSRFData;
import com.github.lbarnkow.minchir.test.testutilities.DefaultTestEnvironmentVariables;
import com.github.lbarnkow.minchir.test.testutilities.FileBasedWireMock;
import com.github.lbarnkow.minchir.test.testutilities.LdapTest;
import com.github.lbarnkow.minchir.test.util.Cookie;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.unboundid.ldap.listener.InMemoryDirectoryServer;

import io.javalin.testtools.TestUtil;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Response;

@FileBasedWireMock(stubs = {"/hydra_wiremock.yaml"})
@LdapTest(ldifFiles = {"users.ldif"})
public class LoginHandlerIntTest {

  private Settings settings;

  @BeforeEach
  void setupEnvironmentVars(WireMockRuntimeInfo wmInfo, InMemoryDirectoryServer ldap) {
    settings = DefaultTestEnvironmentVariables.build2(wmInfo.getHttpBaseUrl(), ldap.getListenPort());
  }

  @Test
  void test_successful_non_skipped_login() throws Exception {
    try (var app = new App().javalinApp(settings)) {
      TestUtil.test(app, (server, client) -> {
        // initial GET to display the login page
        var getResponse = client.get("/login?login_challenge=non_skipped_login_flow");
        var csrf = verifyLoginPageResponse(getResponse);

        // submit login form data via POST
        client.setOkHttp(new OkHttpClient().newBuilder().followRedirects(false).build());
        var postResponse = client.request("/login", builder -> {
          var formData = new MultipartBody.Builder() //
              .setType(MultipartBody.FORM) //
              .addFormDataPart("csrf_token", csrf.getToken()) //
              .addFormDataPart("login_challenge", "non_skipped_login_flow") //
              .addFormDataPart("username", "ldaptest1") //
              .addFormDataPart("password", "123") //
              .addFormDataPart("totp", "45") //
              .addFormDataPart("rememberme", "true") //
              .addFormDataPart("login", "Submit") //
              .build();

          builder.addHeader("Cookie", "csrf_cookie=" + csrf.getCookie()).post(formData);
        });

        // a successful login should redirect to ory hydra
        assertThat(postResponse.code()).isEqualTo(MOVED_TEMPORARILY_302);
        verify(putRequestedFor(urlPathEqualTo("/oauth2/auth/requests/login/accept")) //
            .withRequestBody(WireMock.containing("ldaptest1")));
      });
    }
  }

  @Test
  void test_successful_skipped_login() throws Exception {
    try (var app = new App().javalinApp(settings)) {
      TestUtil.test(app, (server, client) -> {
        client.setOkHttp(new OkHttpClient().newBuilder().followRedirects(false).build());
        var getResponse = client.get("/login?login_challenge=skipped_login_flow");

        // a successful login should redirect to ory hydra
        assertThat(getResponse.code()).isEqualTo(MOVED_TEMPORARILY_302);
        verify(putRequestedFor(urlPathEqualTo("/oauth2/auth/requests/login/accept")) //
            .withRequestBody(WireMock.containing("skip_subject")));
      });
    }
  }

  @Test
  void test_unsuccessful_non_skipped_login() throws Exception {
    try (var app = new App().javalinApp(settings)) {
      TestUtil.test(app, (server, client) -> {
        // initial GET to display the login page
        var getResponse = client.get("/login?login_challenge=non_skipped_login_flow");
        var csrf = verifyLoginPageResponse(getResponse);

        // submit login form data via POST
        client.setOkHttp(new OkHttpClient().newBuilder().followRedirects(false).build());
        var postResponse = client.request("/login", builder -> {
          var formData = new MultipartBody.Builder() //
              .setType(MultipartBody.FORM) //
              .addFormDataPart("csrf_token", csrf.getToken()) //
              .addFormDataPart("login_challenge", "non_skipped_login_flow") //
              .addFormDataPart("username", "ldaptest1") //
              .addFormDataPart("password", "wrong") //
              .addFormDataPart("totp", "fail") //
              .addFormDataPart("rememberme", "true") //
              .addFormDataPart("login", "Submit") //
              .build();

          builder.addHeader("Cookie", "csrf_cookie=" + csrf.getCookie()).post(formData);
        });

        // a failed login should display the login form again.
        assertThat(postResponse.code()).isEqualTo(UNAUTHORIZED_401);
        var postBody = postResponse.body().string();
        assertThat(postBody).contains("<!-- LOGIN -->");
        assertThat(postBody).contains("csrf_token");
        assertThat(postBody).contains(csrf.getToken());
        assertThat(postBody).contains("login_challenge");
        assertThat(postBody).contains("non_skipped_login_flow");
        assertThat(postBody).contains("alert-danger");
        assertThat(postBody).contains("ldaptest1");
        assertThat(postBody).contains("checked");
      });
    }
  }

  @Test
  void test_cancelled_non_skipped_login() throws Exception {
    try (var app = new App().javalinApp(settings)) {
      TestUtil.test(app, (server, client) -> {
        // initial GET to display the login page
        var getResponse = client.get("/login?login_challenge=non_skipped_login_flow");
        var csrf = verifyLoginPageResponse(getResponse);

        // submit login form data via POST
        client.setOkHttp(new OkHttpClient().newBuilder().followRedirects(false).build());
        var postResponse = client.request("/login", builder -> {
          var formData = new MultipartBody.Builder() //
              .setType(MultipartBody.FORM) //
              .addFormDataPart("csrf_token", csrf.getToken()) //
              .addFormDataPart("login_challenge", "non_skipped_login_flow") //
              .addFormDataPart("cancel", "Cancel") //
              .build();

          builder.addHeader("Cookie", "csrf_cookie=" + csrf.getCookie()).post(formData);
        });

        // a cancelled login should redirect to ory hydra
        assertThat(postResponse.code()).isEqualTo(MOVED_TEMPORARILY_302);
        verify(putRequestedFor(urlPathEqualTo("/oauth2/auth/requests/login/reject")) //
            .withRequestBody(WireMock.containing("access_denied")));
      });
    }
  }

  private CSRFData verifyLoginPageResponse(Response response) throws IOException {
    assertThat(response.code()).isEqualTo(200);
    var getBody = response.body().string();
    assertThat(getBody).contains("<!-- LOGIN -->");
    assertThat(getBody).contains("csrf_token");
    assertThat(getBody).contains("login_challenge");
    assertThat(getBody).contains("non_skipped_login_flow");
    var cookie = new Cookie("csrf_cookie", response.headers("Set-Cookie"));
    assertThat(cookie.getValue()).isNotNull();
    assertThat(cookie.isSecure()).isTrue();
    assertThat(cookie.isHttpOnly()).isTrue();
    assertThat(cookie.getPath()).isEqualTo("/login");
    assertThat(cookie.getMaxAge()).isEqualTo("" + (settings.getConfig().getCsrf().getTotpTtlSeconds() * 2));
    var csrf_token = extractCsrfTokenFromFormInput(getBody);
    var csrf_cookie = cookie.getValue();
    verify(getRequestedFor(urlPathEqualTo("/oauth2/auth/requests/login")));

    return new CSRFData(csrf_cookie, csrf_token);
  }

  private String extractCsrfTokenFromFormInput(String html) {
    // type="hidden" name="csrf_token" value="${csrf_token}"
    var p = Pattern.compile("type=.hidden.\\s+name=.csrf_token.\\s+value=.([^\"]+)");
    var m = p.matcher(html);
    if (m.find()) {
      return m.group(1);
    } else {
      return "";
    }
  }
}
