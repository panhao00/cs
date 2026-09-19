package com.crs.repository;

import com.crs.domain.SystemSetting;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SystemSettingRepository extends JpaRepository<SystemSetting, Long> {
    Optional<SystemSetting> findBySettingKey(String settingKey);

    List<SystemSetting> findBySettingKeyIn(List<String> settingKeys);
}
