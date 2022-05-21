package com.github.lbarnkow.minchir.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.lbarnkow.minchir.util.YamlLoader;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class Scopes {

  private final Map<String, List<String>> scopes;

  public List<String> getClaims(String scope) {
    return scopes.get(scope);
  }

  public static Scopes load(String... paths) {
    try {
      var files = YamlLoader.fromFiles(paths);
      var mergedTable = new HashMap<String, List<String>>();

      for (var file : files) {
        mergeTables(mergedTable, file);
      }

      return new Scopes(mergedTable);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static void mergeTables(Map<String, List<String>> base, Map<String, ?> overlay) {
    for (var scope : overlay.entrySet()) {
      var key = scope.getKey();
      var claims = (List<?>) scope.getValue();

      var baseClaims = new ArrayList<String>();
      base.put(key, baseClaims);

      for (var claim : claims) {
        baseClaims.add(claim.toString());
      }
    }
  }
}
