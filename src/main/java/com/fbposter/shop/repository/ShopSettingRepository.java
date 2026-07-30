package com.fbposter.shop.repository;

import com.fbposter.shop.domain.ShopSetting;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShopSettingRepository extends JpaRepository<ShopSetting, String> {
  List<ShopSetting> findAllByOrderBySettingKeyAsc();
}
