package com.fbposter.shop.service;

import com.fbposter.shop.domain.OrderStatus;
import com.fbposter.shop.domain.ShopOrder;
import com.fbposter.shop.repository.ShopOrderRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OrderExpiryScheduler {

  private final ShopOrderRepository orderRepository;

  public OrderExpiryScheduler(ShopOrderRepository orderRepository) {
    this.orderRepository = orderRepository;
  }

  /** Mỗi 30s đánh dấu đơn PENDING đã quá hạn → EXPIRED. */
  @Scheduled(fixedDelayString = "30000")
  @Transactional
  public void expirePendingOrders() {
    Instant now = Instant.now();
    List<ShopOrder> pending = orderRepository.findByStatusOrderByCreatedAtDesc(OrderStatus.PENDING);
    for (ShopOrder order : pending) {
      if (order.getExpiresAt() != null && order.getExpiresAt().isBefore(now)) {
        order.setStatus(OrderStatus.EXPIRED);
        orderRepository.save(order);
      }
    }
  }
}
