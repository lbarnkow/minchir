package com.github.lbarnkow.minchir.handlers.request;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.lbarnkow.minchir.config.Settings;
import com.github.lbarnkow.minchir.handlers.before.csrf.CSRFHandler;
import com.github.lbarnkow.minchir.hydra.OryHydraAdminApi;
import com.github.lbarnkow.minchir.hydra.model.consent.ConsentChallenge;
import com.github.lbarnkow.minchir.util.ContextUtil;

import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;

public class ConsentHandler extends AbstractHandler {

  private static final Logger LOG = LoggerFactory.getLogger(ConsentHandler.class);

  public ConsentHandler(Settings settings, OryHydraAdminApi hydraAdmin, CSRFHandler csrfHandler) throws Exception {
    super(settings, hydraAdmin, csrfHandler);
  }

  @Override
  public String getRoute() {
    return "/consent";
  }

  @Override
  public boolean needsCsrf() {
    return true;
  }

  @Override
  public void doGet(Context ctx) throws Exception {
    LOG.debug("Handling {} for {}", ctx.req.getMethod(), getRoute());

    var consentChallenge = hydraAdmin.fetchConsentChallenge(ContextUtil.getQueryParam(ctx, "consent_challenge"));
    ctx.attribute("consent_challenge", consentChallenge.getChallenge());

    if (consentChallenge.isSkip()) {
      LOG.debug("Skipping consent form as requested by ory hydra via consent challenge!");
      var acceptResponse = hydraAdmin.acceptConsent(ctx, consentChallenge.getChallenge(),
          consentChallenge.getRequested_scope(), consentChallenge.getRequested_access_token_audience(), true);
      ctx.redirect(acceptResponse.getRedirect_to());
    } else {
      LOG.debug("Rendering consent page.");

      var client = consentChallenge.getClient().getClient_name();
      if (client == null || client.isBlank()) {
        client = consentChallenge.getClient().getClient_id();
      }

      ctx.attribute("client", client);
      ctx.attribute("subject", consentChallenge.getSubject());

      prepareScopesAndClaims(ctx, consentChallenge);

      super.doGet(ctx);
    }
  }

  @Override
  public void doPost(Context ctx) throws Exception {
    LOG.debug("Handling {} for {}", ctx.req.getMethod(), getRoute());

    var consentChallenge = ContextUtil.getFormParam(ctx, "consent_challenge");
    var consent = !ContextUtil.getFormParam(ctx, "consent", "").isEmpty();
    var cancel = !ContextUtil.getFormParam(ctx, "cancel", "").isEmpty();

    if (!cancel && !consent) {
      LOG.error("POST request is missing values for consent/cancel form buttons!");
      throw new BadRequestResponse();
    }

    if (cancel) {
      LOG.debug("User submitted via cancel button.");
      var rejectResponse = hydraAdmin.rejectConsent(ctx, consentChallenge);
      ctx.redirect(rejectResponse.getRedirect_to());
      return;
    }

    var rememberMe = Boolean.parseBoolean(ContextUtil.getFormParam(ctx, "rememberconsent", "false"));
    var consentChallengeObj = hydraAdmin.fetchConsentChallenge(consentChallenge);

    LOG.info("Subject '{}' consented for client '{}'.", consentChallengeObj.getSubject(),
        consentChallengeObj.getClient().getClient_id());
    var acceptResponse = hydraAdmin.acceptConsent(ctx, consentChallengeObj.getChallenge(),
        consentChallengeObj.getRequested_scope(), consentChallengeObj.getRequested_access_token_audience(), rememberMe);
    ctx.redirect(acceptResponse.getRedirect_to());
  }

  private void prepareScopesAndClaims(Context ctx, ConsentChallenge challenge) {
    var language = ctx.req.getLocale().getLanguage();

    var translations = settings.getTranslations().get(language);
    var scopes = challenge.getRequested_scope();

    var result = new LinkedHashMap<String, List<String>>();

    for (var scope : scopes) {
      var scopeText = translations.get("scope_" + scope);
      var scopeClaims = new ArrayList<String>();

      var claims = settings.getScopes().getClaims(scope);
      if (claims != null) {
        for (var claim : claims) {
          var claimText = translations.get("claim_" + claim);
          scopeClaims.add(claimText);
        }
      }

      result.put(scopeText, scopeClaims);
    }

    ctx.attribute("scopes", result);
  }
}
