package com.github.lbarnkow.minchir.test.trash;

import org.junit.jupiter.api.Test;

import com.github.lbarnkow.minchir.test.testutilities.LdapTest;
import com.github.lbarnkow.minchir.test.testutilities.baseclasses.BaseTest;
import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchScope;

@BaseTest
@LdapTest(ldifFiles = {"users.ldif"})
public class LdapStuff {
  @Test
  void ldap(InMemoryDirectoryServer ldap) throws Exception {
    // SSLSocketFactory ssf = SSLContext.getDefault().getSocketFactory();
    try (var con = new LDAPConnection("localhost", ldap.getListenPort())) {
      con.bind("cn=binduser,ou=Users,dc=myorg,dc=com", "s3cr3t");

      var srq = new SearchRequest(//
          "dc=myorg,dc=com", //
          SearchScope.SUB, //
          "(&(uid=ldaptest1)(objectClass=person))", //
          "dn", "memberOf", "givenName", "sn", "mail");
      var search = con.search(srq);

      var otherDn = search.getSearchEntries().get(0).getDN();
      System.err.println(search.getSearchEntries().get(0));
      System.err.println(otherDn);
    }
  }
}
