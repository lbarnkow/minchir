package com.github.lbarnkow.minchir.handlers.before.csrf;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.lbarnkow.minchir.config.Config.Cookies;
import com.github.lbarnkow.minchir.config.Config.Csrf;
import com.github.lbarnkow.minchir.config.Settings;

import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.Cookie;
import io.javalin.http.Handler;
import io.javalin.http.SameSite;

public class CSRFHandler implements Handler {

  public static final String CSRF_TOKEN_FORM_PARAM_NAME = "csrf_token";
  public static final String CSRF_TOKEN_COOKIE_NAME = "csrf_cookie";

  private static final Logger LOG = LoggerFactory.getLogger(CSRFHandler.class);

  private final CSRFSupplier csrfSupplier;

  private final Cookies cookiesConf;
  private final Csrf csrfConf;

  public CSRFHandler(Settings settings) throws InvalidKeyException, NoSuchAlgorithmException {
    cookiesConf = settings.getConfig().getServer().getCookies();
    csrfConf = settings.getConfig().getCsrf();

    var totpKey = csrfConf.getTotpKey() != null ? csrfConf.getTotpKey() : CSRFSupplier.generateKey();
    var hmacKey = csrfConf.getHmacKey() != null ? csrfConf.getHmacKey() : CSRFSupplier.generateKey();
    csrfSupplier = new CSRFSupplier(csrfConf.getTotpTtlSeconds(), totpKey, hmacKey);
  }

  @Override
  public void handle(Context ctx) throws Exception {
    if ("GET".equals(ctx.method())) {
      createCsrfTokens(ctx);

    } else if ("POST".equals(ctx.method())) {
      verifiyCsrfTokens(ctx);
    }
  }

  public void createCsrfTokens(Context ctx) throws InvalidKeyException {
    var csrf = csrfSupplier.generate();

    var cookie = new Cookie( //
        CSRF_TOKEN_COOKIE_NAME, // name
        csrf.getCookie(), // value
        cookiesConf.getPath() + ctx.path(), // path
        csrfConf.getTotpTtlSeconds() * 2, // max age
        cookiesConf.getSecure(), // secure
        0, // version
        cookiesConf.getHttpOnly(), // http only
        null, // comment
        null, // domain
        SameSite.STRICT); // same site
    ctx.cookie(cookie);
    ctx.attribute("csrf_token", csrf.getToken());
  }

  private void verifiyCsrfTokens(Context ctx) {
    try {
      csrfSupplier.verify(ctx.formParam(CSRF_TOKEN_FORM_PARAM_NAME), ctx.cookie(CSRF_TOKEN_COOKIE_NAME));

      ctx.attribute(CSRF_TOKEN_FORM_PARAM_NAME, ctx.formParam(CSRF_TOKEN_FORM_PARAM_NAME));
    } catch (Exception e) {
      LOG.debug("CSRF verification failed!");
      throw new BadRequestResponse();
    }
  }
}
