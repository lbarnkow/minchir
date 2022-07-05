package com.github.lbarnkow.minchir.testutilities.ldap;

import java.io.BufferedReader;
import java.io.StringReader;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import com.github.lbarnkow.minchir.testutilities.Resource;
import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryDirectoryServerSnapshot;
import com.unboundid.ldif.LDIFReader;

public class LdapTestExtension
    implements Extension, ParameterResolver, BeforeAllCallback, BeforeEachCallback, AfterAllCallback {
  private InMemoryDirectoryServer ldap = null;
  private InMemoryDirectoryServerSnapshot snapshot = null;

  @Override
  public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    return parameterContext.getTarget().orElse("").getClass().isAnnotationPresent(LdapTest.class) && //
        parameterContext.getParameter().getType() == InMemoryDirectoryServer.class;
  }

  @Override
  public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    if (parameterContext.getParameter().getType() == InMemoryDirectoryServer.class) {
      return ldap;
    }

    throw new IllegalStateException();
  }

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    var annotation = context.getRequiredTestClass().getAnnotation(LdapTest.class);

    var config = new InMemoryDirectoryServerConfig(annotation.baseDNs()[0]);
    config.setSchema(null);
    ldap = new InMemoryDirectoryServer(config);

    for (String ldif : annotation.ldifFiles()) {
      try (var reader = new LDIFReader(new BufferedReader(new StringReader(Resource.load(ldif))))) {
        ldap.importFromLDIF(true, reader);
      }
    }

    snapshot = ldap.createSnapshot();
    ldap.startListening();
  }

  @Override
  public void afterAll(ExtensionContext context) throws Exception {
    ldap.shutDown(true);
  }

  @Override
  public void beforeEach(ExtensionContext context) throws Exception {
    ldap.restoreSnapshot(snapshot);
  }
}
