package com.github.lbarnkow.minchir.test.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.lbarnkow.minchir.util.TemplateModel;

public class CombinedModelTest {

  private final Map<String, String> lowPrio = Map.of( //
      "key-1", "low-1", //
      "key-2", "low-2", //
      "key-3", "low-3");

  private final Map<String, Object> highPrio = Map.of( //
      "key-1", "high-1", //
      "key-5", "high-5", //
      "key-6", "high-6");

  private Map<String, Object> combined;

  @BeforeEach
  public void setup() {
    combined = new TemplateModel(highPrio, lowPrio);
  }

  @Test
  public void testEmpty() {
    assertThat(combined).isNotEmpty();
  }

  @Test
  public void testContains() {
    assertThat(combined).containsOnlyKeys("key-1", "key-2", "key-3", "key-5", "key-6");
    assertThat(combined).containsValues("high-1", "low-2", "low-3", "high-5", "high-6");

    assertThat(combined).doesNotContainValue("low-1");
  }

  @Test
  public void testGet() {
    assertThat(combined).containsEntry("key-1", "high-1");
    assertThat(combined).containsEntry("key-2", "low-2");
    assertThat(combined).containsEntry("key-3", "low-3");
    assertThat(combined).containsEntry("key-5", "high-5");
    assertThat(combined).containsEntry("key-6", "high-6");
  }

  @Test
  public void testPut() {
    Assertions.assertThrows(UnsupportedOperationException.class, () -> combined.put("a", "b"));
  }

  @Test
  public void testPutAll() {
    Assertions.assertThrows(UnsupportedOperationException.class, () -> combined.putAll(Map.of("a", "b", "x", "y")));
  }

  @Test
  public void testRemove() {
    Assertions.assertThrows(UnsupportedOperationException.class, () -> combined.remove("key-1"));
  }

  @Test
  public void testClear() {
    Assertions.assertThrows(UnsupportedOperationException.class, () -> combined.clear());
  }

  @Test
  public void testKeySet() {
    Set<String> keys = new HashSet<>();

    keys.addAll(highPrio.keySet());
    keys.addAll(lowPrio.keySet());

    assertThat(combined.keySet()).containsOnly(toArray(keys, String.class));
  }

  @Test
  public void testValues() {
    Set<Object> values = Set.of("high-1", "low-2", "low-3", "high-5", "high-6");

    assertThat(combined.values()).containsOnly(toArray(values, Object.class));
  }

  private <T> T[] toArray(Collection<T> collection, Class<T> clazz) {
    @SuppressWarnings("unchecked")
    T[] t = (T[]) Array.newInstance(clazz, 0);
    return collection.toArray(t);
  }
}
