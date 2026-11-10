package com.dat.ai_receptionist_web.service.Iot; import lombok.RequiredArgsConstructor; import org.springframework.scheduling.annotation.Scheduled; import org.springframework.stereotype.Component;
@Component @RequiredArgsConstructor public class DeviceAlertScheduler { private final DeviceService devices; @Scheduled(fixedDelay=30000) public void check(){devices.checkOffline();} }
