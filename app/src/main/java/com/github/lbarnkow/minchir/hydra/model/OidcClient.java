package com.github.lbarnkow.minchir.hydra.model;

import java.util.List;

import lombok.Data;

@Data
public class OidcClient {
  private String client_id;
  private String client_name;
  private List<String> redirect_uris;
  private List<String> grant_types;
  private List<String> response_types;
  private String scope;
  private List<String> audience;
  private String owner;
  private String policy_uri;
  private List<String> allowed_cors_origins;
  private String tos_uri;
  private String client_uri;
  private String logo_uri;
  private List<String> contacts;
  private long client_secret_expires_at;
  private String subject_type;
  private String token_endpoint_auth_method;
  private String userinfo_signed_response_alg;
  private String created_at;
  private String updated_at;
}
