package com.fbposter.shop.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "shop_settings")
public class ShopSetting {

  @Id
  @Column(length = 80)
  private String settingKey;

  @Column(length = 2000)
  private String settingValue;

  @Column(nullable = false)
  private boolean secret = false;

  @Column(nullable = false)
  private Instant updatedAt = Instant.now();

  public String getSettingKey() {
    return settingKey;
  }

  public void setSettingKey(String settingKey) {
    this.settingKey = settingKey;
  }

  public String getSettingValue() {
    return settingValue;
  }

  public void setSettingValue(String settingValue) {
    this.settingValue = settingValue;
  }

  public boolean isSecret() {
    return secret;
  }

  public void setSecret(boolean secret) {
    this.secret = secret;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }
}
