package com.github.lbarnkow.minchir.util;

import lombok.Data;
import lombok.EqualsAndHashCode;

@SuppressWarnings("serial")
@Data
@EqualsAndHashCode(callSuper = true)
public class SystemExitException extends Exception {
  private final int status;
}
