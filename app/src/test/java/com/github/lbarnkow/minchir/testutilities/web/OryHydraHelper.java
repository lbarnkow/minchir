package com.github.lbarnkow.minchir.testutilities.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jetty.http.HttpMethod.DELETE;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.util.ssl.SslContextFactory;

public class OryHydraHelper {
  public static final String ORY_AUTH_ENDPOINT = "/oauth2/auth";
  public static final String ORY_LOGOUT_ENDPOINT = "/oauth2/sessions/logout";

  public static final String ORY_ADMIN_CONSENT_SESSIONS_ENDPOINT = "/oauth2/auth/sessions/consent";

  public static String getAuthorizationCodeFlowUrl( //
      String oryBaseUrl, String clientId, String responseType, //
      String scope, String redirectUri, String state) {

    return oryBaseUrl + //
        ORY_AUTH_ENDPOINT + "?" + //
        urlencode("client_id", clientId) + "&" + //
        urlencode("response_type", responseType) + "&" + //
        urlencode("scope", scope) + "&" + //
        urlencode("redirect_uri", redirectUri) + "&" + //
        urlencode("state", state);
  }

  public static String getLogoutUrl(String oryBaseUrl) {
    return oryBaseUrl + ORY_LOGOUT_ENDPOINT;
  }

  public static void cleanConsentSessions(String oryAdminBaseUrl) throws Exception {
    var sslContextFactory = new SslContextFactory.Client();
    sslContextFactory.setTrustStorePath("../docker/certs/truststore.jks");
    sslContextFactory.setTrustStoreType("jks");
    sslContextFactory.setTrustStorePassword("changeit");
    var client = new HttpClient(sslContextFactory);
    client.start();
    var res = client.newRequest(oryAdminBaseUrl + //
        ORY_ADMIN_CONSENT_SESSIONS_ENDPOINT + "?" + //
        urlencode("subject", "janedoe") + "&" + //
        urlencode("all", "true")) //
        .method(DELETE).send();
    assertThat(res.getStatus()).isEqualTo(HttpStatus.NO_CONTENT_204);
    client.stop();
  }

  private static String urlencode(String key, String value) {
    return URLEncoder.encode(key, StandardCharsets.UTF_8) + "=" + //
        URLEncoder.encode(value, StandardCharsets.UTF_8);
  }
}
