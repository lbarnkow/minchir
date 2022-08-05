package com.github.lbarnkow.minchir.handlers.request;

import static io.javalin.apibuilder.ApiBuilder.before;
import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.post;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.lbarnkow.minchir.config.Settings;
import com.github.lbarnkow.minchir.handlers.before.csrf.CSRFHandler;
import com.github.lbarnkow.minchir.hydra.OryHydraAdminApi;
import com.github.lbarnkow.minchir.util.TemplateModel;

import io.javalin.apibuilder.EndpointGroup;
import io.javalin.http.Context;
import io.javalin.http.Handler;

public abstract class AbstractHandler implements EndpointGroup {

  private static final Logger LOG = LoggerFactory.getLogger(AbstractHandler.class);

  protected final Settings settings;

  protected final OryHydraAdminApi hydraAdmin;
  private final CSRFHandler csrfHandler;

  private final String template;


  public AbstractHandler(Settings settings, OryHydraAdminApi hydraAdmin, CSRFHandler csrfHandler) {
    this.settings = settings;
    this.hydraAdmin = hydraAdmin;
    this.csrfHandler = csrfHandler;

    if (needsCsrf() && csrfHandler == null) {
      throw new RuntimeException(String.format(
          "%s needs a csrf handler, but no handler was supplied in constructor call!", getClass().getSimpleName()));
    }

    template = settings.getConfig().getServer().getAssetsPath("templates") + getRoute() + ".vtl";
  }

  public abstract String getRoute();

  public abstract boolean needsCsrf();

  @Override
  public void addEndpoints() {
    if (csrfHandler != null) {
      before(getRoute(), csrfHandler);
    }
    get(getRoute(), ctx -> logExceptions(ctx, this::doGet));
    post(getRoute(), ctx -> logExceptions(ctx, this::doPost));
  }

  private void logExceptions(Context ctx, Handler handler) throws Exception {
    try {
      handler.handle(ctx);
    } catch (Exception e) {
      LOG.error("Error handling {} for {}! {}", ctx.req.getMethod(), getRoute(), e.getMessage());
      LOG.trace("", e);
      throw e;
    }
  }

  public void doGet(Context ctx) throws Exception {
    var language = ctx.req.getLocale().getLanguage();
    LOG.debug("Rendering template '{}' in language '{}'.", template, language);
    var model = new TemplateModel(ctx.attributeMap(), settings.getTranslations().get(language));
    ctx.render(template, model);
  }

  public abstract void doPost(Context ctx) throws Exception;
}
