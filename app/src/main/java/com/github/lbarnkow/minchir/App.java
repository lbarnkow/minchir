package com.github.lbarnkow.minchir;

import org.apache.velocity.app.VelocityEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.lbarnkow.minchir.config.Config;
import com.github.lbarnkow.minchir.config.Scopes;
import com.github.lbarnkow.minchir.config.Settings;
import com.github.lbarnkow.minchir.config.Translations;
import com.github.lbarnkow.minchir.handlers.before.csrf.CSRFHandler;
import com.github.lbarnkow.minchir.handlers.request.ConsentHandler;
import com.github.lbarnkow.minchir.handlers.request.LoginHandler;
import com.github.lbarnkow.minchir.handlers.request.LogoutHandler;
import com.github.lbarnkow.minchir.hydra.impl.OryHydraAdminApiImpl;
import com.github.lbarnkow.minchir.util.SystemExitException;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import io.javalin.plugin.rendering.template.JavalinVelocity;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;

@Command(name = "minchir")
public class App {
  private static final Logger LOG = LoggerFactory.getLogger(App.class);

  @Option( //
      names = {"--help"}, //
      usageHelp = true, //
      description = {"Show this help message and exit." //
      } //
  )
  boolean usageHelpRequested;

  @Option( //
      required = true, //
      names = "--config", //
      arity = "1..*", //
      description = {"A configuration file to load. At least one configuration file must " + //
          "be specified! This parameter may be used mutliple times, in which case all " + //
          "configuration files will be merged in to one configuration with latter files " + //
          "overriding settings in former files." //
      } //
  )
  private String[] configFiles;

  @Option( //
      required = true, //
      names = "--translation", //
      description = {"A translation file to load. At least one translation file must " + //
          "be specified! This parameter may be used mutliple times, in which case all " + //
          "translation files will be merged in to one translation-table with latter files " + //
          "overriding translations in former files." //
      } //
  )
  private String[] translationFiles;

  @Option( //
      required = true, //
      names = "--scopes", //
      arity = "1..*", //
      description = {"A scopes file to load. At least one scope file must " + //
          "be specified! This parameter may be used mutliple times, in which case all " + //
          "scope files will be merged in to one scope-table with latter files " + //
          "overriding scopes in former files." //
      } //
  )
  private String[] scopesFiles;

  private static void help(CommandLine cli, int status) throws SystemExitException {
    cli.usage(System.out);
    throw new SystemExitException(status);
  }

  public static void main(String... args) throws Exception {
    try {
      setupCli(args);
    } catch (SystemExitException e) {
      System.exit(e.getStatus());
    } catch (Exception e) {
      LOG.error("Fatal error, shutting down! {}", e.getMessage());
      LOG.trace("", e);
      System.exit(1);
    }
  }

  public static void setupCli(String... args) throws Exception {
    var app = new App();
    var cli = new CommandLine(app);

    try {
      cli.parseArgs(args);
      if (app.usageHelpRequested)
        help(cli, 0);
    } catch (ParameterException e) {
      help(cli, 1);
    }

    app.javalinApp().start();
  }

  public Javalin javalinApp() throws Exception {
    var settings = new Settings( //
        Config.load(configFiles), //
        Translations.load(translationFiles), //
        Scopes.load(scopesFiles) //
    );

    return javalinApp(settings);
  }

  public Javalin javalinApp(Settings settings) throws Exception {
    var config = settings.getConfig();
    final var translations = settings.getTranslations();

    var velocityEngine = new VelocityEngine();
    velocityEngine.init();

    JavalinVelocity.configure(velocityEngine);

    var app = Javalin.create(c -> {
      c.addStaticFiles(config.getServer().getAssetsPath("static"), Location.EXTERNAL);
    });
    app.jettyServer().setServerPort(config.getServer().getPort());

    for (final var code : new int[] {400, 404, 500}) {
      final var template = String.format("%s/%s.vtl", config.getServer().getAssetsPath("templates"), code);
      app.error(code, ctx -> {
        var lang = ctx.req.getLocale().getLanguage();
        ctx.render(template, translations.get(lang));
      });
    }

    var hydraAdmin = new OryHydraAdminApiImpl(settings);
    var csrfHandler = new CSRFHandler(settings);

    app.routes(new LoginHandler(settings, hydraAdmin, csrfHandler));
    app.routes(new ConsentHandler(settings, hydraAdmin, csrfHandler));
    app.routes(new LogoutHandler(settings, hydraAdmin, csrfHandler));

    return app;
  }
}
