package com.rickenbazolo.refund.api;

import com.rickenbazolo.refund.application.*;
import java.time.Clock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class RefundLabApplication {
  public static void main(String[] args) {
    SpringApplication.run(RefundLabApplication.class, args);
  }

  @Bean
  OrderRepository orders() {
    return DemoFixtures.orders();
  }

  @Bean
  PolicyRepository policies() {
    return DemoFixtures.policies();
  }

  @Bean
  OrderService orderService(OrderRepository r) {
    return new OrderService(r);
  }

  @Bean
  EligibilityService eligibilityService(PolicyRepository p) {
    return new EligibilityService(p, Clock.systemUTC());
  }

  @Bean
  RefundService refundService(OrderService o, EligibilityService e) {
    return new RefundService(o, e);
  }
}
