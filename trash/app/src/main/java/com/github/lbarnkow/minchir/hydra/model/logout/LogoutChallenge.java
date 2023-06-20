package com.github.lbarnkow.minchir.hydra.model.logout;

import lombok.Data;

@Data
public class LogoutChallenge {
  private String subject;
  private String sid;
  private String request_url;
  private boolean rp_initiated;
}
