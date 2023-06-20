package com.github.lbarnkow.minchir.hydra.model.consent;

import java.util.List;

import com.github.lbarnkow.minchir.hydra.model.BaseAccept;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class ConsentAccept extends BaseAccept {
  private final List<String> grant_scope;
  private final List<String> grant_access_token_audience;
}
