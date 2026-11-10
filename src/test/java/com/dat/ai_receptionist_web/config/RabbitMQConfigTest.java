package com.dat.ai_receptionist_web.config;

import com.dat.ai_receptionist_web.dto.Training.command.FaceCheckInCommandMessage;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConverter;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RabbitMQConfigTest {
    @Test
    void convertsFaceCheckInCommandWithRequestedAtForRabbitListeners() {
        MessageConverter converter = new RabbitMQConfig().jsonMessageConverter();
        FaceCheckInCommandMessage command = new FaceCheckInCommandMessage(
                UUID.randomUUID(), UUID.randomUUID(), 0.91f,
                LocalDateTime.of(2026, 11, 11, 2, 23, 36));

        Message message = converter.toMessage(command, new MessageProperties());
        Object restored = converter.fromMessage(message);

        assertThat(restored).isEqualTo(command);
    }
}
