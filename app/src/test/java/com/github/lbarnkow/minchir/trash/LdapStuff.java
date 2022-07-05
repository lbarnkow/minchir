package com.github.lbarnkow.minchir.trash;

import org.junit.jupiter.api.Test;

import com.github.lbarnkow.minchir.testutilities.baseclasses.BaseTest;
import com.github.lbarnkow.minchir.testutilities.ldap.LdapTest;
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
      con.bind("cn=bind_user,ou=Users,dc=minchir,dc=lbarnkow", "s3cr3t");

      var srq = new SearchRequest(//
          "dc=minchir,dc=lbarnkow", //
          SearchScope.SUB, //
          "(&(uid=janedoe)(objectClass=person))", //
          "dn", "memberOf", "givenName", "sn", "mail");
      var search = con.search(srq);

      var otherDn = search.getSearchEntries().get(0).getDN();
      System.err.println(search.getSearchEntries().get(0));
      System.err.println(otherDn);
    }
  }
}
