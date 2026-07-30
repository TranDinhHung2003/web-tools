package com.fbposter.shop.service;

import com.fbposter.shop.config.AppProperties;
import com.fbposter.shop.domain.ShopSetting;
import com.fbposter.shop.repository.ShopSettingRepository;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SettingsService {

  public static final String KEY_APP_NAME = "app.name";
  public static final String KEY_BASE_URL = "app.baseUrl";
  public static final String KEY_ADMIN_EMAIL = "app.adminEmail";
  public static final String KEY_MAIL_FROM = "app.mailFrom";
  public static final String KEY_RESEND_API_KEY = "app.resendApiKey";
  public static final String KEY_OTP_EXPIRE = "app.otpExpireMinutes";
  public static final String KEY_ORDER_EXPIRE = "app.orderExpireMinutes";
  public static final String KEY_SEPAY_API_KEY = "app.sepay.apiKey";
  public static final String KEY_SEPAY_BANK = "app.sepay.bankCode";
  public static final String KEY_SEPAY_ACCOUNT = "app.sepay.accountNumber";
  public static final String KEY_SEPAY_ACCOUNT_NAME = "app.sepay.accountName";
  public static final String KEY_SEPAY_PREFIX = "app.sepay.paymentPrefix";

  public static final Set<String> KNOWN_KEYS =
      Set.of(
          KEY_APP_NAME,
          KEY_BASE_URL,
          KEY_ADMIN_EMAIL,
          KEY_MAIL_FROM,
          KEY_RESEND_API_KEY,
          KEY_OTP_EXPIRE,
          KEY_ORDER_EXPIRE,
          KEY_SEPAY_API_KEY,
          KEY_SEPAY_BANK,
          KEY_SEPAY_ACCOUNT,
          KEY_SEPAY_ACCOUNT_NAME,
          KEY_SEPAY_PREFIX);

  public static final Set<String> SECRET_KEYS = Set.of(KEY_RESEND_API_KEY, KEY_SEPAY_API_KEY);

  private final ShopSettingRepository repo;
  private final AppProperties props;

  public SettingsService(ShopSettingRepository repo, AppProperties props) {
    this.repo = repo;
    this.props = props;
  }

  @Transactional
  public void bootstrap() {
    seedIfAbsent(KEY_APP_NAME, props.getName(), false);
    seedIfAbsent(KEY_BASE_URL, props.getBaseUrl(), false);
    seedIfAbsent(KEY_ADMIN_EMAIL, props.getAdminEmail(), false);
    seedIfAbsent(KEY_MAIL_FROM, props.getMailFrom(), false);
    seedIfAbsent(KEY_RESEND_API_KEY, props.getResendApiKey(), true);
    seedIfAbsent(KEY_OTP_EXPIRE, String.valueOf(props.getOtpExpireMinutes()), false);
    seedIfAbsent(KEY_ORDER_EXPIRE, String.valueOf(props.getOrderExpireMinutes()), false);
    seedIfAbsent(KEY_SEPAY_API_KEY, props.getSepay().getApiKey(), true);
    seedIfAbsent(KEY_SEPAY_BANK, props.getSepay().getBankCode(), false);
    seedIfAbsent(KEY_SEPAY_ACCOUNT, props.getSepay().getAccountNumber(), false);
    seedIfAbsent(KEY_SEPAY_ACCOUNT_NAME, props.getSepay().getAccountName(), false);
    seedIfAbsent(KEY_SEPAY_PREFIX, props.getSepay().getPaymentPrefix(), false);
    applyAllFromDb();
  }

  public Map<String, String> asMap() {
    Map<String, String> map = new LinkedHashMap<>();
    for (ShopSetting s : repo.findAllByOrderBySettingKeyAsc()) {
      map.put(s.getSettingKey(), s.getSettingValue() == null ? "" : s.getSettingValue());
    }
    return map;
  }

  public List<ShopSetting> extras() {
    return repo.findAllByOrderBySettingKeyAsc().stream()
        .filter(s -> !KNOWN_KEYS.contains(s.getSettingKey()))
        .toList();
  }

  public String get(String key) {
    return repo.findById(key).map(ShopSetting::getSettingValue).orElse("");
  }

  @Transactional
  public void saveKnown(
      String appName,
      String baseUrl,
      String adminEmail,
      String mailFrom,
      String resendApiKey,
      Integer otpExpireMinutes,
      Integer orderExpireMinutes,
      String sepayApiKey,
      String bankCode,
      String accountNumber,
      String accountName,
      String paymentPrefix) {
    put(KEY_APP_NAME, appName, false);
    put(KEY_BASE_URL, baseUrl, false);
    put(KEY_ADMIN_EMAIL, adminEmail, false);
    put(KEY_MAIL_FROM, mailFrom, false);
    if (resendApiKey != null && !resendApiKey.isBlank() && !isMasked(resendApiKey)) {
      put(KEY_RESEND_API_KEY, resendApiKey.trim(), true);
    }
    if (otpExpireMinutes != null && otpExpireMinutes > 0) {
      put(KEY_OTP_EXPIRE, String.valueOf(otpExpireMinutes), false);
    }
    if (orderExpireMinutes != null && orderExpireMinutes > 0) {
      put(KEY_ORDER_EXPIRE, String.valueOf(orderExpireMinutes), false);
    }
    if (sepayApiKey != null && !sepayApiKey.isBlank() && !isMasked(sepayApiKey)) {
      put(KEY_SEPAY_API_KEY, sepayApiKey.trim(), true);
    }
    put(KEY_SEPAY_BANK, bankCode, false);
    put(KEY_SEPAY_ACCOUNT, accountNumber, false);
    put(KEY_SEPAY_ACCOUNT_NAME, accountName, false);
    put(KEY_SEPAY_PREFIX, paymentPrefix, false);
    applyAllFromDb();
  }

  @Transactional
  public void upsert(String key, String value, boolean secret) {
    if (key == null || key.isBlank()) {
      throw new IllegalArgumentException("Key không được trống");
    }
    String k = key.trim();
    if (value != null && isMasked(value) && SECRET_KEYS.contains(k)) {
      return;
    }
    put(k, value == null ? "" : value.trim(), secret || SECRET_KEYS.contains(k));
    applyAllFromDb();
  }

  @Transactional
  public void delete(String key) {
    if (key == null || key.isBlank()) {
      return;
    }
    String k = key.trim();
    if (KNOWN_KEYS.contains(k) && !SECRET_KEYS.contains(k)) {
      throw new IllegalArgumentException("Không xoá được khoá hệ thống: " + k);
    }
    repo.deleteById(k);
    if (KEY_RESEND_API_KEY.equals(k)) {
      props.setResendApiKey("");
    } else if (KEY_SEPAY_API_KEY.equals(k)) {
      props.getSepay().setApiKey("");
    }
  }

  public String maskedSecret(String key) {
    String v = get(key);
    if (v == null || v.isBlank()) {
      return "";
    }
    if (v.length() <= 8) {
      return "••••••••";
    }
    return v.substring(0, 4) + "••••••••" + v.substring(v.length() - 4);
  }

  private void seedIfAbsent(String key, String value, boolean secret) {
    if (!repo.existsById(key)) {
      put(key, value == null ? "" : value, secret);
    }
  }

  private void put(String key, String value, boolean secret) {
    ShopSetting s = repo.findById(key).orElseGet(ShopSetting::new);
    s.setSettingKey(key);
    s.setSettingValue(value == null ? "" : value);
    s.setSecret(secret);
    s.setUpdatedAt(Instant.now());
    repo.save(s);
  }

  private void applyAllFromDb() {
    Map<String, String> map = asMap();
    apply(map, KEY_APP_NAME, props::setName);
    apply(map, KEY_BASE_URL, props::setBaseUrl);
    apply(map, KEY_ADMIN_EMAIL, props::setAdminEmail);
    apply(map, KEY_MAIL_FROM, props::setMailFrom);
    apply(map, KEY_RESEND_API_KEY, props::setResendApiKey);
    if (map.containsKey(KEY_OTP_EXPIRE)) {
      try {
        props.setOtpExpireMinutes(Integer.parseInt(map.get(KEY_OTP_EXPIRE).trim()));
      } catch (NumberFormatException ignored) {
      }
    }
    if (map.containsKey(KEY_ORDER_EXPIRE)) {
      try {
        props.setOrderExpireMinutes(Integer.parseInt(map.get(KEY_ORDER_EXPIRE).trim()));
      } catch (NumberFormatException ignored) {
      }
    }
    apply(map, KEY_SEPAY_API_KEY, props.getSepay()::setApiKey);
    apply(map, KEY_SEPAY_BANK, props.getSepay()::setBankCode);
    apply(map, KEY_SEPAY_ACCOUNT, props.getSepay()::setAccountNumber);
    apply(map, KEY_SEPAY_ACCOUNT_NAME, props.getSepay()::setAccountName);
    apply(map, KEY_SEPAY_PREFIX, props.getSepay()::setPaymentPrefix);
  }

  private static void apply(Map<String, String> map, String key, java.util.function.Consumer<String> setter) {
    if (map.containsKey(key)) {
      setter.accept(map.get(key) == null ? "" : map.get(key));
    }
  }

  private static boolean isMasked(String value) {
    return value != null && value.contains("••••");
  }
}
