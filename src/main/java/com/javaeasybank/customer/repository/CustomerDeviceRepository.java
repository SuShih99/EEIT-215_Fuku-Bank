package com.javaeasybank.customer.repository;

import com.javaeasybank.customer.entity.CustomerDevice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerDeviceRepository extends JpaRepository<CustomerDevice, Long> {
    Optional<CustomerDevice> findByCustomerIdAndDeviceFingerprint(String customerId, String deviceFingerprint);
    Optional<CustomerDevice> findByDeviceIdAndCustomerId(Long deviceId, String customerId);
    List<CustomerDevice> findByCustomerIdOrderByLastSeenAtDesc(String customerId);
}
