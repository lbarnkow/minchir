package com.github.lbarnkow.minchir.handlers.request;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.lbarnkow.minchir.config.Settings;
import com.github.lbarnkow.minchir.handlers.before.csrf.CSRFHandler;
import com.github.lbarnkow.minchir.hydra.OryHydraAdminApi;
import com.github.lbarnkow.minchir.util.ContextUtil;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPConnectionPool;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPURL;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchScope;

import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.HttpCode;

public class LoginHandler extends AbstractHandler {

  private static final Logger LOG = LoggerFactory.getLogger(LoginHandler.class);

  private final SSLSocketFactory sslSocketFactory = SSLContext.getDefault().getSocketFactory();
  private final LDAPConnectionPool ldap;

  public LoginHandler(Settings settings, OryHydraAdminApi hydraAdmin, CSRFHandler csrfHandler) throws Exception {
    super(settings, hydraAdmin, csrfHandler);

    var ldapConfig = settings.getConfig().getLdap();
    LDAPURL ldapUrl = new LDAPURL(ldapConfig.getServerUrl());
    LDAPConnection con;

    if ("ldaps".equals(ldapUrl.getScheme())) {
      LOG.debug("Using TLS secured sockets.");
      con = new LDAPConnection(sslSocketFactory, ldapUrl.getHost(), ldapUrl.getPort(), ldapConfig.getBindDn(),
          ldapConfig.getBindPassword());
    } else {
      LOG.warn("Connecting to LDAP server through INSECURE, UNENCRYPTED socket!");
      con = new LDAPConnection(ldapUrl.getHost(), ldapUrl.getPort(), ldapConfig.getBindDn(),
          ldapConfig.getBindPassword());
    }
    ldap = new LDAPConnectionPool(con, 1);

    LOG.info("Successfully established LDAP connection and performed bind request.");
  }

  @Override
  public String getRoute() {
    return "/login";
  }

  @Override
  public boolean needsCsrf() {
    return true;
  }

  @Override
  public void doGet(Context ctx) throws Exception {
    LOG.debug("Handling {} for {}", ctx.req.getMethod(), getRoute());

    var loginChallenge = hydraAdmin.fetchLoginChallenge(ContextUtil.getQueryParam(ctx, "login_challenge"));
    ctx.attribute("login_challenge", loginChallenge.getChallenge());

    if (loginChallenge.isSkip()) {
      LOG.debug("Skipping login form as requested by ory hydra via login challenge!");
      var acceptResponse =
          hydraAdmin.acceptLogin(ctx, loginChallenge.getChallenge(), loginChallenge.getSubject(), true);
      ctx.redirect(acceptResponse.getRedirect_to());
    } else {
      LOG.debug("Rendering login page.");
      super.doGet(ctx);
    }
  }

  @Override
  public void doPost(Context ctx) throws Exception {
    LOG.debug("Handling {} for {}", ctx.req.getMethod(), getRoute());

    var loginChallenge = ContextUtil.getFormParam(ctx, "login_challenge");
    var login = !ContextUtil.getFormParam(ctx, "login", "").isEmpty();
    var cancel = !ContextUtil.getFormParam(ctx, "cancel", "").isEmpty();

    if (!cancel && !login) {
      LOG.error("POST request is missing values for login/cancel form buttons!");
      throw new BadRequestResponse();
    }

    if (cancel) {
      LOG.debug("User submitted via cancel button.");
      var rejectResponse = hydraAdmin.rejectLogin(ctx, loginChallenge);
      ctx.redirect(rejectResponse.getRedirect_to());
      return;
    }

    LOG.debug("User submitted via login button.");
    var username = ContextUtil.getFormParam(ctx, "username");
    var password = ContextUtil.getFormParam(ctx, "password");
    var totp = ContextUtil.getFormParam(ctx, "totp");
    var rememberMe = Boolean.parseBoolean(ContextUtil.getFormParam(ctx, "rememberme", "false"));

    // Check the validity of the LoginChallenge *before* testing the credentials
    var loginChallengeObj = hydraAdmin.fetchLoginChallenge(loginChallenge);

    if (checkLogin(username, password, totp)) {
      LOG.info("User '{}' successfully logged in.", username);
      var acceptResponse = hydraAdmin.acceptLogin(ctx, loginChallengeObj.getChallenge(), username, rememberMe);
      ctx.redirect(acceptResponse.getRedirect_to());
      return;
    }

    // Try again, friend...
    ctx.attribute("error_bad_credentials", true);
    ctx.attribute("login_challenge", loginChallenge);
    ctx.attribute("username", username);
    ctx.attribute("rememberme", rememberMe);
    super.doGet(ctx);
    ctx.status(HttpCode.UNAUTHORIZED);
  }

  private boolean checkLogin(String username, String password, String totp) throws LDAPException {
    var ldapConfig = settings.getConfig().getLdap();
    LOG.debug("Trying to authenticate user '{}' against LDAP server.", username);

    LDAPConnection con = null;
    try {
      con = ldap.getConnection();

      var filter = Filter.createANDFilter( //
          Filter.createEqualityFilter("objectClass", ldapConfig.getUserSearchObjectClass()), //
          Filter.createEqualityFilter(ldapConfig.getUserAttributeUid(), username) //
      );
      var searchRequest = new SearchRequest( //
          ldapConfig.getUserSearchBaseDn(), SearchScope.SUB, filter, //
          "dn", ldapConfig.getUserAttributeUid(), ldapConfig.getUserAttributeGivenName(), //
          ldapConfig.getUserAttributeSurname(), ldapConfig.getUserAttributeMail());
      var search = con.search(searchRequest);

      var success = false;
      var count = search.getEntryCount();

      if (count == 1) {
        var userDn = search.getSearchEntries().get(0).getDN();
        try {
          var bind = con.bind(userDn, password + totp);

          success = (bind.getResultCode() == ResultCode.SUCCESS);
        } catch (LDAPException e) {
          LOG.info("Failed bind request for user '{}'; reason: {}", username, e.getResultString());
        }
      } else if (count == 0) {
        LOG.info("User '{}' not found in LDAP search!", username);
      } else {
        LOG.warn("Found too many objects ({}) matching the (&(objectClass={})({}={}))!", //
            count, ldapConfig.getUserSearchObjectClass(), ldapConfig.getUserAttributeUid(), username);
      }

      ldap.releaseAndReAuthenticateConnection(con);

      return success;
    } catch (Exception e) {
      ldap.releaseDefunctConnection(con);
      throw e;
    }
  }
}
