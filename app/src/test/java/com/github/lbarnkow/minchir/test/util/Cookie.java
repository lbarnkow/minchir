package com.github.lbarnkow.minchir.test.util;

import static java.lang.Boolean.TRUE;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class Cookie {
  private final String value;
  private final String path;
  private final String expires;
  private final String maxAge;
  private final boolean secure;
  private final boolean httpOnly;
  private final String sameSite;

  public Cookie(String name, List<String> headers) {
    Map<String, String> map = null;

    for (String header : headers) {
      map = parse(header);

      if (map.containsKey(name)) {
        break;
      } else {
        map = null;
      }
    }

    if (map == null) {
      map = new HashMap<>();
    }

    value = map.getOrDefault(name, "");
    path = map.get("Path");
    expires = map.get("Expires");
    maxAge = map.get("Max-Age");
    secure = map.containsKey("Secure");
    httpOnly = map.containsKey("HttpOnly");
    sameSite = map.get("SameSite");
  }

  private Map<String, String> parse(String header) {
    var result = new HashMap<String, String>();
    var parts = header.split(";");

    for (String part : parts) {
      var trimmed = part.trim();
      if (trimmed.contains("=")) {
        var split = singleSplit(trimmed);
        result.put(split[0], split[1]);
      } else {
        result.put(trimmed, TRUE.toString());
      }
    }

    return result;
  }

  private String[] singleSplit(String s) {
    var i = s.indexOf("=");
    return new String[] { //
        s.substring(0, i), //
        s.substring(i + 1, s.length()) //
    };
  }
}
