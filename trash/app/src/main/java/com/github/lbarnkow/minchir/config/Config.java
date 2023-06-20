package com.github.lbarnkow.minchir.config;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.yaml.snakeyaml.Yaml;

import lombok.Data;

@Data
public class Config {

  private Server server;
  private Csrf csrf;
  private Hydra hydra;
  private Ldap ldap;

  @Data
  public static class Server {
    private Integer port;
    private String assetsPath;

    public String getAssetsPath(String subfolder) {
      return assetsPath + File.separator + subfolder + File.separator;
    }
  }

  @Data
  public static class Csrf {
    private Integer totpTtlSeconds;
    private String totpKey;
    private String hmacKey;
  }

  @Data
  public static class Hydra {
    private String adminUrl;
    private Long timeoutMilliseconds;
    private Long rememberForSeconds;
  }

  @Data
  public static class Ldap {
    private String serverUrl;
    private String bindDn;
    private String bindPassword;
    private String userSearchBaseDn;
    private String userSearchObjectClass;
    private String userAttributeUid;
    private String userAttributeGivenName;
    private String userAttributeSurname;
    private String userAttributeMail;
  }

  public static Config load(String... paths) {
    try {
      var configs = loadFiles(paths, Config.class);
      var result = configs.get(0);

      for (var overlay : configs) {
        if (overlay == result)
          continue;
        mergeObjects(result, overlay);
      }

      return result;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static <T> List<T> loadFiles(String[] paths, Class<T> clazz) throws IOException {
    var yaml = new Yaml();
    var result = new ArrayList<T>();

    for (var path : paths) {
      try (var reader = new FileReader(new File(path), StandardCharsets.UTF_8)) {
        result.add(yaml.loadAs(reader, clazz));
      }
    }

    return result;
  }

  private static final List<Class<?>> SIMPLE_TYPES = List.of( //
      Boolean.class, //
      Byte.class, //
      Character.class, //
      Short.class, //
      Integer.class, //
      Long.class, //
      Float.class, //
      Double.class, //
      String.class);

  private static void mergeObjects(Object base, Object overlay)
      throws IllegalArgumentException, IllegalAccessException {
    var fields = base.getClass().getDeclaredFields();

    for (var field : fields) {
      if ((field.getModifiers() & Modifier.STATIC) != 0)
        continue;

      var baseVal = field.get(base);
      var overlayVal = field.get(overlay);

      if (overlayVal == null)
        continue;

      if (baseVal == null) {
        field.set(base, overlayVal);
        continue;
      }

      if (SIMPLE_TYPES.contains(field.getType())) {
        field.set(base, overlayVal);
        continue;
      }

      mergeObjects(baseVal, overlayVal);
    }
  }
}
