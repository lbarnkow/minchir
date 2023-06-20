package com.github.lbarnkow.minchir.hydra.model;

import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public abstract class BaseReject {
  private final String error;
  private final String error_description;
}
