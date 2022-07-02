package com.github.lbarnkow.minchir.test.inttest;

import java.io.File;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.SilentCssErrorHandler;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.javascript.SilentJavaScriptErrorListener;

public class WebClientHelper {
  public static WebClient init() throws Exception {
    return init(BrowserVersion.FIREFOX);
  }

  public static WebClient init(BrowserVersion browser) throws Exception {
    try (final WebClient webClient = new WebClient(browser)) {
      var trustStore = new File("../docker/certs/truststore.jks").toURI().toURL();

      var options = webClient.getOptions();
      options.setSSLTrustStore(trustStore, "changeit", "jks");
      options.setRedirectEnabled(true);
      options.setCssEnabled(false);
      options.setJavaScriptEnabled(false);
      options.setThrowExceptionOnScriptError(false);
      options.setFetchPolyfillEnabled(true);
      options.setThrowExceptionOnFailingStatusCode(false);
      options.setPrintContentOnFailingStatusCode(false);
      webClient.setJavaScriptErrorListener(new SilentJavaScriptErrorListener());
      webClient.setCssErrorHandler(new SilentCssErrorHandler());

      return webClient;
    }
  }
}
