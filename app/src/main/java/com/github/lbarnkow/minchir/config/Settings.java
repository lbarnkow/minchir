package com.github.lbarnkow.minchir.config;

import lombok.Data;

@Data
public class Settings {
  private final Config config;
  private final Translations translations;
  private final Scopes scopes;
}
