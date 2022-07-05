package com.github.lbarnkow.minchir.testutilities.wiremock;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(FileBasedWireMockExtension.class)
public @interface FileBasedWireMock {
  String[] stubs();
}
