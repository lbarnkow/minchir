package com.github.lbarnkow.minchir.hydra.model;

import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public abstract class BaseAccept {
  private final boolean remember;
  private final long remember_for;
}
