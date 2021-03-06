package com.github.lbarnkow.minchir.test.testutilities;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(LdapTestExtension.class)
public @interface LdapTest {
  String[] baseDNs() default {"dc=myorg,dc=com"};

  String[] ldifFiles();
}
