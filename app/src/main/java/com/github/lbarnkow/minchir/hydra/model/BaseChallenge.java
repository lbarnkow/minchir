package com.github.lbarnkow.minchir.hydra.model;

import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class BaseChallenge {
  private String challenge;
  private List<String> requested_scope;
  private List<String> requested_access_token_audience;
  private boolean skip;
  private String subject;
  @SuppressWarnings("rawtypes")
  private Map oidc_context;
  private OidcClient client;
  private String request_url;
}
