package com.github.lbarnkow.minchir.hydra;

import java.util.List;

import com.github.lbarnkow.minchir.hydra.model.consent.ConsentAcceptResponse;
import com.github.lbarnkow.minchir.hydra.model.consent.ConsentChallenge;
import com.github.lbarnkow.minchir.hydra.model.consent.ConsentRejectResponse;
import com.github.lbarnkow.minchir.hydra.model.login.LoginAcceptResponse;
import com.github.lbarnkow.minchir.hydra.model.login.LoginChallenge;
import com.github.lbarnkow.minchir.hydra.model.login.LoginRejectResponse;
import com.github.lbarnkow.minchir.hydra.model.logout.LogoutAcceptResponse;
import com.github.lbarnkow.minchir.hydra.model.logout.LogoutChallenge;

import io.javalin.http.Context;

public interface OryHydraAdminApi {
  LoginChallenge fetchLoginChallenge(String loginChallenge) throws Exception;

  LoginAcceptResponse acceptLogin(Context ctx, String loginChallenge, String subject, boolean remember)
      throws Exception;

  LoginRejectResponse rejectLogin(Context ctx, String loginChallenge) throws Exception;

  ConsentChallenge fetchConsentChallenge(String consentChallenge) throws Exception;

  ConsentAcceptResponse acceptConsent(Context ctx, String consentChallenge, List<String> grantScope,
      List<String> grantAccessTokenAudience, boolean remember) throws Exception;

  ConsentRejectResponse rejectConsent(Context ctx, String consentChallenge) throws Exception;

  LogoutChallenge fetchLogoutChallenge(String logoutChallenge) throws Exception;

  LogoutAcceptResponse acceptLogout(Context ctx, String logoutChallenge) throws Exception;

  void rejectLogout(Context ctx, String logoutChallenge) throws Exception;
}
