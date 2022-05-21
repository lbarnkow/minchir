package com.github.lbarnkow.minchir.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TemplateModel implements Map<String, Object> {

  private Map<String, Object> map = new HashMap<>();

  // TODO: escape / sanitize values?
  public TemplateModel(Map<String, Object> runtimeVars, Map<String, String> translations) {
    map.putAll(translations);
    if (runtimeVars != null) {
      map.putAll(runtimeVars);
    }
  }

  @Override
  public int size() {
    return map.size();
  }

  @Override
  public boolean isEmpty() {
    return map.isEmpty();
  }

  @Override
  public boolean containsKey(Object key) {
    return map.containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    return map.containsValue(value);
  }

  @Override
  public Object get(Object key) {
    return map.get(key);
  }

  @Override
  public String put(String key, Object value) {
    throw new UnsupportedOperationException("Can't modify a " + getClass().getSimpleName() + "!");
  }

  @Override
  public String remove(Object key) {
    throw new UnsupportedOperationException("Can't modify a " + getClass().getSimpleName() + "!");
  }

  @Override
  public void putAll(Map<? extends String, ? extends Object> m) {
    throw new UnsupportedOperationException("Can't modify a " + getClass().getSimpleName() + "!");
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException("Can't modify a " + getClass().getSimpleName() + "!");
  }

  @Override
  public Set<String> keySet() {
    return map.keySet();
  }

  @Override
  public Collection<Object> values() {
    return map.values();
  }

  @Override
  public Set<Entry<String, Object>> entrySet() {
    return map.entrySet();
  }
}
