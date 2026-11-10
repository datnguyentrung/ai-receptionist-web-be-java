package com.dat.ai_receptionist_web.repository.Iot;
import com.dat.ai_receptionist_web.domain.Iot.DeviceEvent;
import org.springframework.data.jpa.repository.*;
import java.util.*;
public interface DeviceEventRepository extends JpaRepository<DeviceEvent, UUID> {
 @Query("select e from DeviceEvent e where e.deviceId=:deviceId and e.eventKind='ALERT' and e.eventCode=:code and e.alertStatus in ('OPEN','ACKNOWLEDGED')")
 Optional<DeviceEvent> findOpenAlert(UUID deviceId, String code);
 @Query("select e from DeviceEvent e where e.deviceId in :deviceIds and e.eventKind='ALERT' and e.eventCode=:code and e.alertStatus in ('OPEN','ACKNOWLEDGED')")
 List<DeviceEvent> findOpenAlerts(Collection<UUID> deviceIds, String code);
 List<DeviceEvent> findByDeviceIdOrderByOccurredAtDesc(UUID deviceId);
}
