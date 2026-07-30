package com.fbposter.shop.repository;

import com.fbposter.shop.domain.LicenseToken;
import com.fbposter.shop.domain.UserAccount;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface LicenseTokenRepository extends JpaRepository<LicenseToken, Long> {
  Optional<LicenseToken> findByToken(String token);

  List<LicenseToken> findByOwnerOrderByCreatedAtDesc(UserAccount owner);

  List<LicenseToken> findAllByOrderByCreatedAtDesc();

  List<LicenseToken> findByRevokedFalseOrderByCreatedAtDesc();

  @Modifying(clearAutomatically = true)
  @Query("delete from LicenseToken t where t.revoked = true")
  int deleteAllRevoked();
}
