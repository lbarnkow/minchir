package com.github.lbarnkow.minchir.testutilities.baseclasses;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@DisabledIfEnvironmentVariable( //
    named = "INTEGRATION_TEST", matches = "ON", //
    disabledReason = "Environment variable INTEGRATION_TEST set to ON.")
@DisabledIfEnvironmentVariable( //
    named = "MOCKED_ENVIRONMENT", matches = "ON", //
    disabledReason = "Environment variable MOCKED_ENVIRONMENT set to ON.")
public @interface BaseTest {
}
