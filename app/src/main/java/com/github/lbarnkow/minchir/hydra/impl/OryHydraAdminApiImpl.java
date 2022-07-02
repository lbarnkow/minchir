package com.github.lbarnkow.minchir.hydra.impl;

import static io.javalin.http.ContentType.APPLICATION_JSON;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.eclipse.jetty.http.HttpHeader.CONTENT_TYPE;
import static org.eclipse.jetty.http.HttpMethod.GET;
import static org.eclipse.jetty.http.HttpMethod.PUT;

import java.util.List;
import java.util.Optional;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.lbarnkow.minchir.config.Config;
import com.github.lbarnkow.minchir.config.Settings;
import com.github.lbarnkow.minchir.hydra.OryHydraAdminApi;
import com.github.lbarnkow.minchir.hydra.model.consent.ConsentAccept;
import com.github.lbarnkow.minchir.hydra.model.consent.ConsentAcceptResponse;
import com.github.lbarnkow.minchir.hydra.model.consent.ConsentChallenge;
import com.github.lbarnkow.minchir.hydra.model.consent.ConsentReject;
import com.github.lbarnkow.minchir.hydra.model.consent.ConsentRejectResponse;
import com.github.lbarnkow.minchir.hydra.model.login.LoginAccept;
import com.github.lbarnkow.minchir.hydra.model.login.LoginAcceptResponse;
import com.github.lbarnkow.minchir.hydra.model.login.LoginChallenge;
import com.github.lbarnkow.minchir.hydra.model.login.LoginReject;
import com.github.lbarnkow.minchir.hydra.model.login.LoginRejectResponse;
import com.github.lbarnkow.minchir.hydra.model.logout.LogoutAcceptResponse;
import com.github.lbarnkow.minchir.hydra.model.logout.LogoutChallenge;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;

public class OryHydraAdminApiImpl implements OryHydraAdminApi {

  private static final String CHALLENGE_NAME_LOGIN = "login_challenge";
  private static final String CHALLENGE_NAME_CONSENT = "consent_challenge";
  private static final String CHALLENGE_NAME_LOGOUT = "logout_challenge";

  private static final Logger LOG = LoggerFactory.getLogger(OryHydraAdminApiImpl.class);

  private static final Gson GSON = new GsonBuilder().create();

  private final Config config;

  private final String hydraLoginChallenge;
  private final String hydraLoginAccept;
  private final String hydraLoginReject;

  private final String hydraConsentChallenge;
  private final String hydraConsentAccept;
  private final String hydraConsentReject;

  private final String hydraLogoutChallenge;
  private final String hydraLogoutAccept;
  private final String hydraLogoutReject;

  private final HttpClient http;

  public OryHydraAdminApiImpl(Settings settings) throws Exception {
    this.config = settings.getConfig();

    hydraLoginChallenge = config.getHydra().getAdminUrl() + "/oauth2/auth/requests/login";
    hydraLoginAccept = hydraLoginChallenge + "/accept";
    hydraLoginReject = hydraLoginChallenge + "/reject";

    hydraConsentChallenge = config.getHydra().getAdminUrl() + "/oauth2/auth/requests/consent";
    hydraConsentAccept = hydraConsentChallenge + "/accept";
    hydraConsentReject = hydraConsentChallenge + "/reject";

    hydraLogoutChallenge = config.getHydra().getAdminUrl() + "/oauth2/auth/requests/logout";
    hydraLogoutAccept = hydraLogoutChallenge + "/accept";
    hydraLogoutReject = hydraLogoutChallenge + "/reject";

    var sslContextFactory = new SslContextFactory.Client();

    if (System.getProperty("javax.net.ssl.trustStore") != null && //
        System.getProperty("javax.net.ssl.trustStorePassword") != null) {
      sslContextFactory.setTrustStorePath(System.getProperty("javax.net.ssl.trustStore"));
      sslContextFactory.setTrustStorePassword(System.getProperty("javax.net.ssl.trustStorePassword"));
    }

    http = new HttpClient(sslContextFactory);
    http.start();
  }

  @Override
  public LoginChallenge fetchLoginChallenge(String loginChallenge) throws Exception {
    LOG.debug("Fetching login challenge '{}' from ory hydra.", loginChallenge);

    var response = contactOryHydra(hydraLoginChallenge, GET, CHALLENGE_NAME_LOGIN, loginChallenge);

    return GSON.fromJson(response, LoginChallenge.class);
  }

  @Override
  public LoginAcceptResponse acceptLogin(Context ctx, String loginChallengeString, String subject, boolean remember)
      throws Exception {
    LOG.debug("Submitting login accept object for challenge '{}' to ory hydra.", loginChallengeString);
    var acceptPayload = LoginAccept.builder() //
        .subject(subject) //
        .remember(remember) //
        .remember_for(config.getHydra().getRememberForSeconds()) //
        .build();

    var response =
        contactOryHydra(hydraLoginAccept, PUT, CHALLENGE_NAME_LOGIN, loginChallengeString, Optional.of(acceptPayload));
    return GSON.fromJson(response, LoginAcceptResponse.class);
  }

  @Override
  public LoginRejectResponse rejectLogin(Context ctx, String loginChallengeString) throws Exception {
    LOG.debug("Submitting login reject object for challenge '{}' to ory hydra.", loginChallengeString);
    var rejectPayload = LoginReject.builder() //
        .error("access_denied") //
        .error_description("The resource owner denied the request") //
        .build();

    var response =
        contactOryHydra(hydraLoginReject, PUT, CHALLENGE_NAME_LOGIN, loginChallengeString, Optional.of(rejectPayload));
    return GSON.fromJson(response, LoginRejectResponse.class);
  }

  @Override
  public ConsentChallenge fetchConsentChallenge(String consentChallenge) throws Exception {
    LOG.debug("Fetching consent challenge '{}' from ory hydra.", consentChallenge);

    var response = contactOryHydra(hydraConsentChallenge, GET, CHALLENGE_NAME_CONSENT, consentChallenge);

    return GSON.fromJson(response, ConsentChallenge.class);
  }

  @Override
  public ConsentAcceptResponse acceptConsent(Context ctx, String consentChallenge, List<String> grantScope,
      List<String> grantAccessTokenAudience, boolean remember) throws Exception {
    LOG.debug("Submitting consent accept object for challenge '{}' to ory hydra.", consentChallenge);
    var acceptPayload = ConsentAccept.builder() //
        .grant_scope(grantScope) //
        .grant_access_token_audience(grantAccessTokenAudience) //
        .remember(remember) //
        .remember_for(0) // TODO: make configurable
        .build();

    var response =
        contactOryHydra(hydraConsentAccept, PUT, CHALLENGE_NAME_CONSENT, consentChallenge, Optional.of(acceptPayload));
    return GSON.fromJson(response, ConsentAcceptResponse.class);
  }

  @Override
  public ConsentRejectResponse rejectConsent(Context ctx, String consentChallenge) throws Exception {
    LOG.debug("Submitting consent reject object for challenge '{}' to ory hydra.", consentChallenge);
    var rejectPayload = ConsentReject.builder() //
        .error("access_denied") //
        .error_description("The resource owner denied the request") //
        .build();

    var response =
        contactOryHydra(hydraConsentReject, PUT, CHALLENGE_NAME_CONSENT, consentChallenge, Optional.of(rejectPayload));
    return GSON.fromJson(response, ConsentRejectResponse.class);
  }

  @Override
  public LogoutChallenge fetchLogoutChallenge(String logoutChallenge) throws Exception {
    LOG.debug("Fetching logout challenge '{}' from ory hydra.", logoutChallenge);

    var response = contactOryHydra(hydraLogoutChallenge, GET, CHALLENGE_NAME_LOGOUT, logoutChallenge);

    return GSON.fromJson(response, LogoutChallenge.class);
  }

  @Override
  public LogoutAcceptResponse acceptLogout(Context ctx, String logoutChallenge) throws Exception {
    LOG.debug("Submitting logout accept object for challenge '{}' to ory hydra.", logoutChallenge);
    var response = contactOryHydra(hydraLogoutAccept, PUT, CHALLENGE_NAME_LOGOUT, logoutChallenge);
    return GSON.fromJson(response, LogoutAcceptResponse.class);
  }

  @Override
  public void rejectLogout(Context ctx, String logoutChallenge) throws Exception {
    LOG.debug("Submitting logout reject object for challenge '{}' to ory hydra.", logoutChallenge);
    contactOryHydra(hydraLogoutReject, PUT, CHALLENGE_NAME_LOGOUT, logoutChallenge);
  }

  private String contactOryHydra(String url, HttpMethod method, String challengeName, String challengeValue)
      throws Exception {
    return contactOryHydra(url, method, challengeName, challengeValue, Optional.empty());
  }

  private String contactOryHydra(String url, HttpMethod method, String challengeName, String challengeValue,
      Optional<Object> body) throws Exception {
    var request = http.newRequest(url) //
        .method(method) //
        .param(challengeName, challengeValue) //
        .header(CONTENT_TYPE, APPLICATION_JSON.getMimeType()) //
        .timeout(config.getHydra().getTimeoutMilliseconds(), MILLISECONDS);

    body.ifPresent(val -> request.content(new StringContentProvider(GSON.toJson(val))));

    var response = request.send();

    if (!isStatusOK(response.getStatus()) || //
        !isMediaTypeOK(response.getMediaType())) {
      LOG.warn("Failed to interact with ory hydra ({} - {}) for {} challenge '{}': {}, {}, {}", //
          method.asString(), //
          body != null ? body.getClass().getSimpleName() : "null", //
          challengeName, //
          challengeValue, //
          response.getStatus(), //
          response.getMediaType(), //
          response.getContentAsString());
      throw new BadRequestResponse();
    }

    LOG.debug(response.getContentAsString());

    return response.getContentAsString();
  }

  private boolean isStatusOK(int status) {
    return status >= HttpStatus.OK_200 && status < HttpStatus.MULTIPLE_CHOICES_300;
  }

  private boolean isMediaTypeOK(String mediaType) {
    return mediaType == null || //
        APPLICATION_JSON.getMimeType().equals(mediaType);
  }
}
