package com.fbposter.shop.config;

import com.fbposter.shop.service.SettingsService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class SettingsBootstrap implements ApplicationRunner {

  private final SettingsService settingsService;

  public SettingsBootstrap(SettingsService settingsService) {
    this.settingsService = settingsService;
  }

  @Override
  public void run(ApplicationArguments args) {
    settingsService.bootstrap();
  }
}
