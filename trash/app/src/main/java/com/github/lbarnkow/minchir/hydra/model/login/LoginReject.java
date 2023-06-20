package com.github.lbarnkow.minchir.hydra.model.login;

import com.github.lbarnkow.minchir.hydra.model.BaseReject;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class LoginReject extends BaseReject {
}
