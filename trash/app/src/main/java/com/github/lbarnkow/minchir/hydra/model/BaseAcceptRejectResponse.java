package com.github.lbarnkow.minchir.hydra.model;

import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class BaseAcceptRejectResponse {
  private String redirect_to;
}
