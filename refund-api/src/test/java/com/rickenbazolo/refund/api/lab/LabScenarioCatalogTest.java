package com.rickenbazolo.refund.api.lab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class LabScenarioCatalogTest {

  private final LabScenarioCatalog catalog = new LabScenarioCatalog();

  @Test
  void shouldContainTenScenariosWithUniqueIds() {
    assertEquals(10, catalog.all().size());
    Set<String> ids = catalog.all().stream().map(LabScenario::id).collect(Collectors.toSet());
    assertEquals(catalog.all().size(), ids.size());
  }

  @Test
  void shouldRejectUnknownScenario() {
    assertThrows(IllegalArgumentException.class, () -> catalog.require("UNKNOWN"));
  }
}
