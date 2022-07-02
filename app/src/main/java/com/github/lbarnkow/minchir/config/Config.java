package com.github.lbarnkow.minchir.config;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import lombok.Data;

@Data
public class Config implements Normalizable {

  private Server server;
  private Csrf csrf;
  private Hydra hydra;
  private Ldap ldap;

  @Override
  public void normalize() {
    server.normalize();
    csrf.normalize();
    hydra.normalize();
    ldap.normalize();
  }

  @Data
  public static class Server implements Normalizable {
    private Integer port;
    private String assetsPath;
    private String fallbackRedirect;
    private Cookies cookies;

    public String getAssetsPath(String subfolder) {
      return assetsPath + File.separator + subfolder + File.separator;
    }

    @Override
    public void normalize() {
      assetsPath = normalizeOsPath(assetsPath);
      cookies.normalize();
    }
  }

  @Data
  public static class Cookies implements Normalizable {
    private Boolean secure;
    private Boolean httpOnly;
    private String path;

    @Override
    public void normalize() {
      path = normalizeUrlPath(path);
    }
  }

  @Data
  public static class Csrf implements Normalizable {
    private Integer totpTtlSeconds;
    private String totpKey;
    private String hmacKey;

    @Override
    public void normalize() {}
  }

  @Data
  public static class Hydra implements Normalizable {
    private String adminUrl;
    private Long timeoutMilliseconds;
    private Long rememberForSeconds;

    @Override
    public void normalize() {
      adminUrl = normalizeUrlPath(adminUrl);
    }
  }

  @Data
  public static class Ldap implements Normalizable {
    private String serverUrl;
    private String bindDn;
    private String bindPassword;
    private String userSearchBaseDn;
    private String userSearchObjectClass;
    private String userAttributeUid;
    private String userAttributeGivenName;
    private String userAttributeSurname;
    private String userAttributeMail;

    @Override
    public void normalize() {}
  }

  public static Config load(String... paths) {
    try {
      var configs = loadFiles(paths, Config.class);
      var result = configs.get(0);

      for (var overlay : configs) {
        if (overlay == result)
          continue;
        mergeObjects(result, overlay);
        LOG.debug("Effective configuration: {}", result);
      }

      result.normalize();

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

  private static final Logger LOG = LoggerFactory.getLogger(Config.class);

  private static void mergeObjects(Object base, Object overlay)
      throws IllegalArgumentException, IllegalAccessException {
    var fields = base.getClass().getDeclaredFields();

    for (var field : fields) {
      if ((field.getModifiers() & Modifier.STATIC) != 0)
        continue;
      if ((field.getModifiers() & Modifier.FINAL) != 0)
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

  private static String normalizeOsPath(String path) {
    return normalizePath(path, File.separator);
  }

  private static String normalizeUrlPath(String path) {
    return normalizePath(path, "/");
  }

  private static String normalizePath(String path, String seperator) {
    while (path.endsWith(seperator)) {
      path = path.substring(0, path.length() - 1);
    }
    return path;
  }
}
