package com.fbposter.shop.web;

import com.fbposter.shop.config.AppProperties;
import com.fbposter.shop.domain.OrderStatus;
import com.fbposter.shop.domain.ShopOrder;
import com.fbposter.shop.domain.UserAccount;
import com.fbposter.shop.service.OrderService;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ShopController {

  private static final DateTimeFormatter VN_DT =
      DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

  private final OrderService orderService;
  private final CurrentUser currentUser;
  private final AppProperties props;

  public ShopController(OrderService orderService, CurrentUser currentUser, AppProperties props) {
    this.orderService = orderService;
    this.currentUser = currentUser;
    this.props = props;
  }

  @PostMapping("/buy/{planId}")
  public String buy(@PathVariable Long planId, RedirectAttributes ra) {
    try {
      UserAccount user = currentUser.require();
      ShopOrder order = orderService.createOrder(user, planId);
      return "redirect:/checkout/" + order.getId();
    } catch (IllegalArgumentException ex) {
      ra.addFlashAttribute("error", ex.getMessage());
      return "redirect:/";
    }
  }

  @GetMapping("/checkout/{orderId}")
  public String checkout(@PathVariable Long orderId, Model model) {
    UserAccount user = currentUser.require();
    ShopOrder order = orderService.refreshExpiry(orderService.requireOwned(orderId, user));
    model.addAttribute("order", order);
    model.addAttribute("qrUrl", orderService.buildQrUrl(order));
    model.addAttribute("bank", props.getSepay());
    model.addAttribute("appName", props.getName());
    model.addAttribute("expireMinutes", props.getOrderExpireMinutes());
    return "checkout";
  }

  @GetMapping("/checkout/{orderId}/status")
  @ResponseBody
  public ResponseEntity<Map<String, Object>> status(@PathVariable Long orderId) {
    UserAccount user = currentUser.require();
    ShopOrder order = orderService.refreshExpiry(orderService.requireOwned(orderId, user));
    boolean paid = order.getStatus() == OrderStatus.PAID;
    boolean expired = order.getStatus() == OrderStatus.EXPIRED;
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("status", order.getStatus().name());
    body.put("paid", paid);
    body.put("expired", expired);
    body.put("token", order.getIssuedToken() == null ? "" : order.getIssuedToken());
    body.put("durationLabel", order.getPlan().durationLabel());
    body.put("planName", order.getPlan().getName());
    body.put("paymentCode", order.getPaymentCode());
    body.put("amountVnd", order.getAmountVnd());
    body.put("buyerEmail", order.getBuyerEmail() == null ? "" : order.getBuyerEmail());
    body.put(
        "expiresAtEpochMs",
        order.getExpiresAt() == null ? 0L : order.getExpiresAt().toEpochMilli());
    body.put(
        "paidAt",
        order.getPaidAt() == null ? "" : VN_DT.format(order.getPaidAt()));
    body.put(
        "createdAt",
        order.getCreatedAt() == null ? "" : VN_DT.format(order.getCreatedAt()));
    body.put(
        "expiresAt",
        order.getExpiresAt() == null ? "" : VN_DT.format(order.getExpiresAt()));
    body.put(
        "sepayTxnId",
        order.getSepayTransactionId() == null ? "" : order.getSepayTransactionId());
    body.put(
        "transferAmount",
        order.getTransferAmount() == null ? order.getAmountVnd() : order.getTransferAmount());
    return ResponseEntity.ok(body);
  }

  @GetMapping("/account/orders")
  public String myOrders(Model model) {
    UserAccount user = currentUser.require();
    model.addAttribute("user", user);
    model.addAttribute("orders", orderService.ordersOf(user));
    model.addAttribute("appName", props.getName());
    return "account-orders";
  }

  @GetMapping("/account/orders/{orderId}")
  public String orderDetail(@PathVariable Long orderId, Model model) {
    UserAccount user = currentUser.require();
    ShopOrder order = orderService.requireOwned(orderId, user);
    model.addAttribute("order", order);
    model.addAttribute("user", user);
    return "account-order-detail";
  }
}
