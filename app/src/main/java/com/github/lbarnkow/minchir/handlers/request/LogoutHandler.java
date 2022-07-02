package com.github.lbarnkow.minchir.handlers.request;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.lbarnkow.minchir.config.Settings;
import com.github.lbarnkow.minchir.handlers.before.csrf.CSRFHandler;
import com.github.lbarnkow.minchir.hydra.OryHydraAdminApi;
import com.github.lbarnkow.minchir.util.ContextUtil;

import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;

public class LogoutHandler extends AbstractHandler {

  private static final Logger LOG = LoggerFactory.getLogger(LogoutHandler.class);

  public LogoutHandler(Settings settings, OryHydraAdminApi hydraAdmin, CSRFHandler csrfHandler) throws Exception {
    super(settings, hydraAdmin, csrfHandler);
  }

  @Override
  public String getRoute() {
    return "/logout";
  }

  @Override
  public boolean needsCsrf() {
    return true;
  }

  @Override
  public void doGet(Context ctx) throws Exception {
    LOG.debug("Handling {} for {}", ctx.req.getMethod(), getRoute());

    var logoutChallengeParam = ContextUtil.getQueryParam(ctx, "logout_challenge");
    var logoutChallenge = hydraAdmin.fetchLogoutChallenge(logoutChallengeParam);
    ctx.attribute("logout_challenge", logoutChallengeParam);

    LOG.debug("Rendering logout page.");
    ctx.attribute("subject", logoutChallenge.getSubject());

    super.doGet(ctx);
  }

  @Override
  public void doPost(Context ctx) throws Exception {
    LOG.debug("Handling {} for {}", ctx.req.getMethod(), getRoute());

    var logoutChallenge = ContextUtil.getFormParam(ctx, "logout_challenge");
    var logout = !ContextUtil.getFormParam(ctx, "logout", "").isEmpty();
    var cancel = !ContextUtil.getFormParam(ctx, "cancel", "").isEmpty();

    if (!cancel && !logout) {
      LOG.error("POST request is missing values for logout/cancel form buttons!");
      throw new BadRequestResponse();
    }

    if (cancel) {
      LOG.debug("User submitted via cancel button.");
      hydraAdmin.rejectLogout(ctx, logoutChallenge);
      ctx.redirect("40404"); // TODO
      return;
    }

    var logoutChallengeObj = hydraAdmin.fetchLogoutChallenge(logoutChallenge);

    LOG.info("Subject '{}' logged out.", logoutChallengeObj.getSubject());
    var acceptResponse = hydraAdmin.acceptLogout(ctx, logoutChallenge);
    ctx.redirect(acceptResponse.getRedirect_to());
  }
}
