package com.github.lbarnkow.minchir.config;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class Translations {
  private final Map<String, Map<String, String>> languages;

  public Map<String, String> get(String language) {
    var result = languages.get(language);
    if (result == null) {
      result = languages.get("en");
    }
    return result;
  }

  public static Translations load(String... paths) {
    try {
      var files = loadFiles(paths);
      var mergedTable = new HashMap<String, Map<String, String>>();

      for (var file : files) {
        mergeTables(mergedTable, file);
      }

      var global = mergedTable.remove("global");
      for (var languageTable : mergedTable.values()) {
        languageTable.putAll(global);
      }

      return new Translations(mergedTable);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static List<Map<String, ?>> loadFiles(String[] paths) throws IOException {
    var yaml = new Yaml();
    var result = new ArrayList<Map<String, ?>>();

    for (var path : paths) {
      try (var reader = new FileReader(new File(path), StandardCharsets.UTF_8)) {
        result.add(yaml.load(reader));
      }
    }

    return result;
  }

  private static void mergeTables(Map<String, Map<String, String>> base, Map<String, ?> overlay) {
    for (var language : overlay.entrySet()) {
      var countryCode = language.getKey();
      @SuppressWarnings("unchecked")
      var overlayTable = (Map<String, ?>) language.getValue();

      var baseTable = base.get(countryCode);
      if (baseTable == null) {
        baseTable = new HashMap<String, String>();
        base.put(countryCode, baseTable);
      }

      for (var entry : overlayTable.entrySet()) {
        baseTable.put(entry.getKey(), entry.getValue().toString());
      }
    }
  }
}

