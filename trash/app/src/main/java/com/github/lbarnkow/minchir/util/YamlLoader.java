package com.github.lbarnkow.minchir.util;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

public class YamlLoader {
  public static List<Map<String, ?>> fromFiles(String[] paths) throws IOException {
    var yaml = new Yaml();
    var result = new ArrayList<Map<String, ?>>();

    for (var path : paths) {
      try (var reader = new FileReader(new File(path), StandardCharsets.UTF_8)) {
        result.add(yaml.load(reader));
      }
    }

    return result;
  }
}
