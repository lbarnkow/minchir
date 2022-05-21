package com.github.lbarnkow.minchir.hydra.model.login;

import com.github.lbarnkow.minchir.hydra.model.BaseChallenge;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class LoginChallenge extends BaseChallenge {
  private String session_id;
}
