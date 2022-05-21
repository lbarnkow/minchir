package com.github.lbarnkow.minchir.hydra.model.consent;

import com.github.lbarnkow.minchir.hydra.model.BaseReject;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class ConsentReject extends BaseReject {
}
