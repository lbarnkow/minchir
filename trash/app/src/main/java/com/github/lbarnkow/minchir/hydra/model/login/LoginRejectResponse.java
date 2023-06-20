package com.github.lbarnkow.minchir.hydra.model.login;

import com.github.lbarnkow.minchir.hydra.model.BaseAcceptRejectResponse;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class LoginRejectResponse extends BaseAcceptRejectResponse {
}
