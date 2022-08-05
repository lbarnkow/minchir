package com.github.lbarnkow.minchir.config;

import java.io.File;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Modifier;

public interface Normalizable {
  public static enum PathType {
    OsPath(File.separator), UrlPath("/");

    private String separator;

    private PathType(String separator) {
      this.separator = separator;
    }

    public String get() {
      return separator;
    }
  }

  @Target(ElementType.FIELD)
  @Retention(RetentionPolicy.RUNTIME)
  public static @interface NormalizeEnd {
    PathType separator();
  }

  default void normalize() throws IllegalArgumentException, IllegalAccessException {
    for (var field : getClass().getDeclaredFields()) {
      if (Modifier.isStatic(field.getModifiers()))
        continue;

      var accessible = field.canAccess(this);
      field.setAccessible(true);
      var value = field.get(this);

      var annotation = field.getDeclaredAnnotation(NormalizeEnd.class);
      if (annotation != null) {
        var normalized = normalizePath(value.toString(), annotation.separator().get());
        field.set(this, normalized);
      }

      field.setAccessible(accessible);

      if (value != null && value instanceof Normalizable n) {
        n.normalize();
      }
    }
  }

  private static String normalizePath(String path, String seperator) {
    if (path == null)
      return null;

    while (path.endsWith(seperator)) {
      path = path.substring(0, path.length() - 1);
    }

    return path;
  }
}
