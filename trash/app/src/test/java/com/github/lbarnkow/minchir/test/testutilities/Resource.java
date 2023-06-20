package com.github.lbarnkow.minchir.test.testutilities;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Scanner;

public class Resource {
  private static final String RESSOURCES_FOLDER = "src/test/resources";

  public static String resourcePath(String file) {
    return Paths.get(RESSOURCES_FOLDER, file).toString();
  }

  public static String load(String path) {
    try {
      // Don't load the resources from classpath via ClassLoader, b/c the native image
      // jvm agent would pick them up and add them to the final executable.
      URL url = Paths.get(RESSOURCES_FOLDER, path).toUri().toURL();

      try (var scanner = new Scanner(url.openStream(), "UTF-8")) {
        return scanner.useDelimiter("\\A").next();
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
