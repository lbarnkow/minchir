package com.github.lbarnkow.minchir.test.testutilities.baseclasses;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@EnabledIfEnvironmentVariable( //
    named = "INTEGRATION_TEST", matches = "ON", //
    disabledReason = "Environment variable INTEGRATION_TEST not set to ON.")
public @interface IntegrationTest {
}
