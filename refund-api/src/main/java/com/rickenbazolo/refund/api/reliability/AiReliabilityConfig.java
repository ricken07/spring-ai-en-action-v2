package com.rickenbazolo.refund.api.reliability;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class AiReliabilityConfig {

  @Bean(destroyMethod = "close")
  ExecutorService aiExecutor() {
    return Executors.newVirtualThreadPerTaskExecutor();
  }

  @Bean
  AiExecutionGuard aiExecutionGuard(
      ExecutorService aiExecutor, @Value("${refund.ai.execution.timeout:20s}") Duration timeout) {
    return new AiExecutionGuard(aiExecutor, timeout);
  }
}
