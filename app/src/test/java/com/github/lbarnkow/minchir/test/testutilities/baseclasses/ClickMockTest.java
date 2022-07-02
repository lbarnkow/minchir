package com.github.lbarnkow.minchir.test.testutilities.baseclasses;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@EnabledIfEnvironmentVariable( //
    named = "MOCKED_ENVIRONMENT", matches = "ON", //
    disabledReason = "Environment variable MOCKED_ENVIRONMENT not set to ON.")
public @interface ClickMockTest {
}
