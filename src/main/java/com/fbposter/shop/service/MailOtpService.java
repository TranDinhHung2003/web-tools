package com.fbposter.shop.service;

import com.fbposter.shop.config.AppProperties;
import com.fbposter.shop.domain.OtpCode;
import com.fbposter.shop.repository.OtpCodeRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MailOtpService {

  private static final String RESEND_URL = "https://api.resend.com/emails";

  private final OtpCodeRepository otpRepo;
  private final AppProperties props;
  private final SecureRandom random = new SecureRandom();
  private final HttpClient http =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

  public MailOtpService(OtpCodeRepository otpRepo, AppProperties props) {
    this.otpRepo = otpRepo;
    this.props = props;
  }

  @Transactional
  public String sendOtp(String email, String purpose) {
    String code = String.format("%06d", random.nextInt(1_000_000));
    OtpCode otp = new OtpCode();
    otp.setEmail(email.trim().toLowerCase());
    otp.setCode(code);
    otp.setPurpose(purpose);
    otp.setExpiresAt(Instant.now().plus(props.getOtpExpireMinutes(), ChronoUnit.MINUTES));
    otpRepo.save(otp);

    String subject;
    String body;
    if ("REGISTER".equalsIgnoreCase(purpose)) {
      subject = "[" + props.getName() + "] Mã OTP đăng ký tài khoản";
      body =
          "Xin chào,\n\n"
              + "Bạn đang đăng ký tài khoản trên "
              + props.getName()
              + ".\n"
              + "Mã OTP xác minh email của bạn là:\n\n"
              + "    "
              + code
              + "\n\n"
              + "Mã có hiệu lực "
              + props.getOtpExpireMinutes()
              + " phút.\n"
              + "Không chia sẻ mã này cho người khác.\n\n"
              + "Nếu bạn không yêu cầu đăng ký, hãy bỏ qua email này.\n\n"
              + "— "
              + props.getName()
              + "\n"
              + "Bản quyền thuộc về TranDinhHung\n"
              + "Zalo/SĐT hỗ trợ: 0981227703\n";
    } else {
      subject = "[" + props.getName() + "] Mã OTP xác minh";
      body =
          "Mã OTP của bạn: "
              + code
              + "\nHiệu lực "
              + props.getOtpExpireMinutes()
              + " phút.\nKhông chia sẻ mã này.";
    }

    sendMail(email, subject, body, "OTP " + purpose + " = " + code);
    return code;
  }

  @Transactional
  public boolean verifyOtp(String email, String purpose, String code) {
    Optional<OtpCode> opt =
        otpRepo.findTopByEmailIgnoreCaseAndPurposeAndUsedFalseOrderByCreatedAtDesc(
            email.trim().toLowerCase(), purpose);
    if (opt.isEmpty()) {
      return false;
    }
    OtpCode otp = opt.get();
    if (otp.isUsed() || otp.getExpiresAt().isBefore(Instant.now())) {
      return false;
    }
    if (!otp.getCode().equals(code.trim())) {
      return false;
    }
    otp.setUsed(true);
    otpRepo.save(otp);
    return true;
  }

  public void sendTokenEmail(String to, String token, String planName, String duration, Instant expiresAt) {
    String subject = "[" + props.getName() + "] Token bản quyền của bạn";
    String body =
        """
        Cảm ơn bạn đã mua hàng!

        Gói: %s
        Thời hạn: %s
        (Đồng hồ thời hạn bắt đầu khi bạn kích hoạt token trên máy)

        MÃ TOKEN:
        %s

        Token của bạn sẽ được lưu trong đơn hàng trên website.

        Hướng dẫn chạy tool:
        1. Mở Facebook Group Poster trên máy của bạn
        2. Dán token vào ô kích hoạt License
        3. Mỗi token chỉ dùng được 1 máy
        4. Xem lại token tại: %s/account/orders

        Hỗ trợ Zalo/SĐT: 0981227703
        © Bản quyền thuộc về TranDinhHung
        """
            .formatted(planName, duration, token, props.getBaseUrl());
    sendMail(to, subject, body, "TOKEN to=" + to + " token=" + token);
  }

  private void sendMail(String to, String subject, String text, String devHint) {
    String apiKey = props.getResendApiKey();
    if (apiKey == null || apiKey.isBlank()) {
      System.out.println("[DEV MAIL] Resend API key chưa cấu hình. " + devHint);
      return;
    }
    try {
      String from = props.getMailFrom() == null || props.getMailFrom().isBlank()
          ? "noreply@fbposter.local"
          : props.getMailFrom();
      String json =
          "{"
              + "\"from\":"
              + jsonString(from)
              + ","
              + "\"to\":["
              + jsonString(to)
              + "],"
              + "\"subject\":"
              + jsonString(subject)
              + ","
              + "\"text\":"
              + jsonString(text)
              + "}";
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(RESEND_URL))
              .timeout(Duration.ofSeconds(20))
              .header("Authorization", "Bearer " + apiKey.trim())
              .header("Content-Type", "application/json")
              .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
              .build();
      HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() >= 300) {
        System.out.println(
            "[RESEND ERROR] status="
                + response.statusCode()
                + " body="
                + response.body()
                + " | "
                + devHint);
      }
    } catch (Exception ex) {
      System.out.println("[RESEND ERROR] " + ex.getMessage() + " | " + devHint);
    }
  }

  private static String jsonString(String value) {
    if (value == null) {
      return "\"\"";
    }
    String escaped =
        value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\r", "\\r")
            .replace("\n", "\\n")
            .replace("\t", "\\t");
    return "\"" + escaped + "\"";
  }
}
