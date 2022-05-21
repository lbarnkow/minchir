package com.github.lbarnkow.minchir.test.testutilities;

import java.util.HashMap;
import java.util.Map;

import com.github.lbarnkow.minchir.config.Config;
import com.github.lbarnkow.minchir.config.Scopes;
import com.github.lbarnkow.minchir.config.Settings;
import com.github.lbarnkow.minchir.config.Translations;

public class DefaultTestEnvironmentVariables {
  public static Map<String, String> build(String oryAdminUrl, int ldapPort) {
    var env = new HashMap<String, String>();

    env.put("SERVER_HYDRA_ADMIN_URL", oryAdminUrl);
    env.put("SERVER_HYDRA_ADMIN_TIMEOUT_MILLISECONDS", "10000");
    env.put("SERVER_HYDRA_ADMIN_REMEMBER_FOR_SECONDS", "604800");

    env.put("SERVER_LDAP_URL", "ldap://localhost:" + ldapPort);

    env.put("SERVER_LDAP_BIND_DN", "cn=binduser,ou=Users,dc=myorg,dc=com");
    env.put("SERVER_LDAP_BIND_PASSWORD", "s3cr3t");
    env.put("SERVER_LDAP_USER_BASE_DN", "dc=myorg,dc=com");
    env.put("SERVER_LDAP_USER_OBJECTCLASS", "person");
    env.put("SERVER_LDAP_USER_UID", "uid");
    env.put("SERVER_LDAP_USER_GIVENNAME", "givenName");
    env.put("SERVER_LDAP_USER_SURNAME", "sn");
    env.put("SERVER_LDAP_USER_MAIL", "mail");

    env.put("TEMPLATE_SITE_TITLE", "IntegrationTest");

    env.put("TEMPLATE_CLIENT", "JUnit Mocking OIDC client");

    return env;
  }

  public static Settings build2(String oryAdminUrl, int ldapPort) {
    var config = Config.load("assets/config/config.yaml");

    config.getHydra().setAdminUrl(oryAdminUrl);

    config.getLdap().setServerUrl("ldap://localhost:" + ldapPort);
    config.getLdap().setBindDn("cn=binduser,ou=Users,dc=myorg,dc=com");
    config.getLdap().setBindPassword("s3cr3t");
    config.getLdap().setUserSearchBaseDn("dc=myorg,dc=com");

    var translations = Translations.load("assets/i18n/translations.yaml");

    var scopes = Scopes.load("assets/scopes/scopes.yaml");

    return new Settings(config, translations, scopes);
  }
}
