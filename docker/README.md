# README.md

## Test glauth (non-TLS)

`$ ldapsearch -H ldap://localhost:10389 -D cn=bind_user,ou=Users,dc=minchir,dc=lbarnkow -w s3cr3t -x -bdc=minchir,dc=lbarnkow objectClass=posixAccount`

## Test openldap (non-TLS)

`$ ldapsearch -H ldap://localhost:20389 -D cn=bind_user,ou=Users,dc=minchir,dc=lbarnkow -w s3cr3t -x -bdc=minchir,dc=lbarnkow objectClass=person`

## Test glauth (TLS)

`$ ldapsearch -H ldaps://127.0.0.1:10636 -D cn=bind_user,ou=Users,dc=minchir,dc=lbarnkow -w s3cr3t -x -bdc=minchir,dc=lbarnkow objectClass=posixAccount`

_Note:_ Using `127.0.0.1` here because ldapsearch would otherwise lookup the real hostname for `localhost` and check that against the SAN and CN of the server certificate.
