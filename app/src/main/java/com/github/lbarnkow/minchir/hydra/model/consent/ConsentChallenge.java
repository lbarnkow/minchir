package com.github.lbarnkow.minchir.hydra.model.consent;

import com.github.lbarnkow.minchir.hydra.model.BaseChallenge;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ConsentChallenge extends BaseChallenge {
  private String login_challenge;
  private String login_session_id;
  private String acr;
}
