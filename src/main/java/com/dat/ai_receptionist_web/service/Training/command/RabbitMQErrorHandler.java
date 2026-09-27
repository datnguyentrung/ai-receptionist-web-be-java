package com.dat.ai_receptionist_web.service.Training.command;

import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.listener.api.RabbitListenerErrorHandler;
import org.springframework.amqp.rabbit.support.ListenerExecutionFailedException;
import org.springframework.stereotype.Component;

@Component("rabbitMQErrorHandler")
@Slf4j
public class RabbitMQErrorHandler implements RabbitListenerErrorHandler {
    @Override
    public Object handleError(
            Message amqpMessage,
            Channel channel,
            org.springframework.messaging.Message<?> message,
            ListenerExecutionFailedException exception
    ) {
        log.error("RabbitMQ command failed queue={} exchange={} routingKey={} body={}",
                amqpMessage.getMessageProperties().getConsumerQueue(),
                amqpMessage.getMessageProperties().getReceivedExchange(),
                amqpMessage.getMessageProperties().getReceivedRoutingKey(),
                new String(amqpMessage.getBody()),
                exception);
        throw new AmqpRejectAndDontRequeueException("Attendance command processing failed", exception);
    }
}
