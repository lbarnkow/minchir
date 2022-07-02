#!/bin/sh

hydra clients create \
    --skip-tls-verify \
    --endpoint https://hydra.minchir:14445/ \
    --callbacks "http://localhost:28080/" \
    --post-logout-callbacks "http://localhost:28080/" \
    --id "oidc-client-1" \
    --name "OIDC Client 1 Name" \
    --scope "openid" \
    --secret "oidc-client-1"

hydra clients list --skip-tls-verify --endpoint https://hydra.minchir:14445/


# Example:
#   hydra clients create -n "my app" -c http://localhost/cb -g authorization_code -r code -a core,foobar

# To encrypt auto generated client secret, use "--pgp-key", "--pgp-key-url" or "--keybase" flag, for example:
#   hydra clients create -n "my app" -g client_credentials -r token -a core,foobar --keybase keybase_username

# Usage:
#   hydra clients create [flags]

# Flags:
#       --allowed-cors-origins strings           The list of URLs allowed to make CORS requests. Requires CORS_ENABLED.
#       --audience strings                       The audience this client is allowed to request
#       --backchannel-logout-callback string     Client URL that will cause the client to log itself out when sent a Logout Token by Hydra.
#       --backchannel-logout-session-required    Boolean flag specifying whether the client requires that a sid (session ID) Claim be included in the Logout Token to identify the client session with the OP when the backchannel-logout-callback is used. If omitted, the default value is false.
#   -c, --callbacks strings                      REQUIRED list of allowed callback URLs
#       --client-uri string                      A URL string of a web page providing information about the client
#       --frontchannel-logout-callback string    Client URL that will cause the client to log itself out when rendered in an iframe by Hydra.
#       --frontchannel-logout-session-required   Boolean flag specifying whether the client requires that a sid (session ID) Claim be included in the Logout Token to identify the client session with the OP when the frontchannel-logout-callback is used. If omitted, the default value is false.
#   -g, --grant-types strings                    A list of allowed grant types (default [authorization_code])
#   -h, --help                                   help for create
#       --id string                              Give the client this id
#       --jwks-uri string                        Define the URL where the JSON Web Key Set should be fetched from when performing the "private_key_jwt" client authentication method
#       --keybase string                         Keybase username for encrypting client secret
#       --logo-uri string                        A URL string that references a logo for the client
#   -n, --name string                            The client's name
#       --pgp-key string                         Base64 encoded PGP encryption key for encrypting client secret
#       --pgp-key-url string                     PGP encryption key URL for encrypting client secret
#       --policy-uri string                      A URL string that points to a human-readable privacy policy document that describes how the deployment organization collects, uses, retains, and discloses personal data
#       --post-logout-callbacks strings          List of allowed URLs to be redirected to after a logout
#   -r, --response-types strings                 A list of allowed response types (default [code])
#   -a, --scope strings                          The scope the client is allowed to request
#       --secret string                          Provide the client's secret
#       --subject-type string                    A identifier algorithm. Valid values are "public" and "pairwise" (default "public")
#       --token-endpoint-auth-method string      Define which authentication method the client may use at the Token Endpoint. Valid values are "client_secret_post", "client_secret_basic", "private_key_jwt", and "none" (default "client_secret_basic")
#       --tos-uri string                         A URL string that points to a human-readable terms of service document for the client that describes a contractual relationship between the end-user and the client that the end-user accepts when authorizing the client

