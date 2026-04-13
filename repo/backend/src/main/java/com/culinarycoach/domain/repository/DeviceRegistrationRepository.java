package com.culinarycoach.domain.repository;

import com.culinarycoach.domain.entity.DeviceRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeviceRegistrationRepository extends JpaRepository<DeviceRegistration, Long> {

    Optional<DeviceRegistration> findByUserIdAndDeviceFingerprint(Long userId, String deviceFingerprint);
}
