package com.github.lbarnkow.minchir.hydra.model.login;

import com.github.lbarnkow.minchir.hydra.model.BaseAccept;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class LoginAccept extends BaseAccept {
  private final String subject;
}
