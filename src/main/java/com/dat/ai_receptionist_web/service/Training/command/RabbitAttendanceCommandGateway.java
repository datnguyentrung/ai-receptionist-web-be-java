package com.dat.ai_receptionist_web.service.Training.command;

import com.dat.ai_receptionist_web.config.RabbitMQConfig;
import com.dat.ai_receptionist_web.dto.Training.command.FaceCheckInCommandMessage;
import com.dat.ai_receptionist_web.dto.Training.command.SessionAttendanceCommandMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RabbitAttendanceCommandGateway implements AttendanceCommandGateway {
    private final RabbitTemplate rabbitTemplate;

    @Override
    public void enqueueFaceCheckIn(FaceCheckInCommandMessage message) {
        log.info("Enqueue face check-in command requestId={} personId={}",
                message.requestId(), message.personId());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.FACE_CHECK_IN_COMMAND_ROUTING_KEY,
                message
        );
    }

    @Override
    public void enqueueSessionAttendance(SessionAttendanceCommandMessage message) {
        log.info("Enqueue session attendance command requestId={} type={}",
                message.requestId(), message.type());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.SESSION_ATTENDANCE_COMMAND_ROUTING_KEY,
                message
        );
    }
}
