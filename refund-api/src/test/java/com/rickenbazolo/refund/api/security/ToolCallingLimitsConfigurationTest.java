package com.rickenbazolo.refund.api.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.ai.model.tool.ToolCallLimitBehavior;
import org.springframework.ai.model.tool.autoconfigure.ToolCallingProperties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

class ToolCallingLimitsConfigurationTest {

  @Test
  void shouldBindSafeToolCallingLimitsFromDemoProfile() throws Exception {
    var environment = new StandardEnvironment();
    var demoProperties =
        new YamlPropertySourceLoader().load("demo", new ClassPathResource("application-demo.yml"));
    demoProperties.forEach(environment.getPropertySources()::addFirst);

    var properties =
        Binder.get(environment)
            .bind("spring.ai.tools", Bindable.of(ToolCallingProperties.class))
            .orElseThrow(IllegalStateException::new);

    assertTrue(properties.isThrowExceptionOnError());
    assertFalse(properties.getResolution().getFallback().isEnabled());
    assertEquals(3, properties.getLimits().getMaxCallsPerToolDefault());
    assertEquals(6, properties.getLimits().getMaxTotalToolCalls());
    assertEquals(ToolCallLimitBehavior.THROW, properties.getLimits().getOnLimitExceeded());
  }
}
