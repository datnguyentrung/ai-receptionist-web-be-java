package com.dat.ai_receptionist_web.repository.Iot;
import com.dat.ai_receptionist_web.domain.Iot.ManagedDevice;
import org.springframework.data.jpa.repository.*;
import java.time.LocalDateTime; import java.util.*;
public interface ManagedDeviceRepository extends JpaRepository<ManagedDevice, UUID> {
 Optional<ManagedDevice> findByDeviceCode(String deviceCode);
 List<ManagedDevice> findByDeviceCodeIn(Collection<String> deviceCodes);
 Optional<ManagedDevice> findByProvisioningPublicId(String provisioningPublicId);
 Optional<ManagedDevice> findByHardwareId(String hardwareId);
 List<ManagedDevice> findByFogGatewayId(UUID fogGatewayId);
 boolean existsByFogGatewayIdOrPairedKioskId(UUID fogGatewayId, UUID pairedKioskId);
 List<ManagedDevice> findByLifecycleStatusAndLastSeenAtBefore(String lifecycleStatus, LocalDateTime cutoff);
}
