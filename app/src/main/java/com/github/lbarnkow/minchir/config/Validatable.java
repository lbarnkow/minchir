package com.github.lbarnkow.minchir.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public interface Validatable {
  @Target(ElementType.FIELD)
  @Retention(RetentionPolicy.RUNTIME)
  public static @interface Nullable {
  }

  default void validate() throws IllegalArgumentException, IllegalAccessException {
    var missing = new ArrayList<String>();
    validate("", missing);

    if (!missing.isEmpty()) {
      Collections.sort(missing);
      var msg = //
          "Missing configuration attribute(s): " + missing.toString() + ".";
      throw new RuntimeException(msg);
    }
  }

  default void validate(String path, List<String> missing) throws IllegalArgumentException, IllegalAccessException {
    for (var field : getClass().getDeclaredFields()) {
      if (Modifier.isStatic(field.getModifiers()))
        continue;

      var accessible = field.canAccess(this);
      field.setAccessible(true);
      var value = field.get(this);
      field.setAccessible(accessible);

      var nullable = (field.getDeclaredAnnotation(Nullable.class) != null);

      if (!nullable && value == null) {
        missing.add(path + field.getName());
      }

      if (value instanceof Validatable v) {
        v.validate("%s%s.".formatted(path, field.getName()), missing);
      }
    }
  }
}
