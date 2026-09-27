package com.dat.ai_receptionist_web.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.ConditionalRejectingErrorHandler;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ErrorHandler;

@Configuration
public class RabbitMQConfig {
    public static final String EXCHANGE_NAME = "taekwondo_exchange";
    public static final String DEAD_LETTER_EXCHANGE_NAME = "taekwondo_dead_letter_exchange";

    public static final String FACE_CHECK_IN_COMMAND_QUEUE = "face_check_in_command_queue";
    public static final String FACE_CHECK_IN_COMMAND_ROUTING_KEY = "face-check-in.command";
    public static final String FACE_CHECK_IN_COMMAND_DLQ = "face_check_in_command_dlq";

    public static final String SESSION_ATTENDANCE_COMMAND_QUEUE = "session_attendance_command_queue";
    public static final String SESSION_ATTENDANCE_COMMAND_ROUTING_KEY = "session-attendance.command";
    public static final String SESSION_ATTENDANCE_COMMAND_DLQ = "session_attendance_command_dlq";

    @Bean
    TopicExchange exchange() {
        return new TopicExchange(EXCHANGE_NAME, true, false);
    }

    @Bean
    DirectExchange deadLetterExchange() {
        return new DirectExchange(DEAD_LETTER_EXCHANGE_NAME, true, false);
    }

    @Bean
    Queue faceCheckInCommandQueue() {
        return QueueBuilder.durable(FACE_CHECK_IN_COMMAND_QUEUE)
                .deadLetterExchange(DEAD_LETTER_EXCHANGE_NAME)
                .deadLetterRoutingKey(FACE_CHECK_IN_COMMAND_ROUTING_KEY)
                .build();
    }

    @Bean
    Binding faceCheckInCommandBinding(Queue faceCheckInCommandQueue, TopicExchange exchange) {
        return BindingBuilder.bind(faceCheckInCommandQueue)
                .to(exchange)
                .with(FACE_CHECK_IN_COMMAND_ROUTING_KEY);
    }

    @Bean
    Queue faceCheckInCommandDeadLetterQueue() {
        return QueueBuilder.durable(FACE_CHECK_IN_COMMAND_DLQ).build();
    }

    @Bean
    Binding faceCheckInCommandDeadLetterBinding(Queue faceCheckInCommandDeadLetterQueue,
                                                DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(faceCheckInCommandDeadLetterQueue)
                .to(deadLetterExchange)
                .with(FACE_CHECK_IN_COMMAND_ROUTING_KEY);
    }

    @Bean
    Queue sessionAttendanceCommandQueue() {
        return QueueBuilder.durable(SESSION_ATTENDANCE_COMMAND_QUEUE)
                .deadLetterExchange(DEAD_LETTER_EXCHANGE_NAME)
                .deadLetterRoutingKey(SESSION_ATTENDANCE_COMMAND_ROUTING_KEY)
                .build();
    }

    @Bean
    Binding sessionAttendanceCommandBinding(Queue sessionAttendanceCommandQueue, TopicExchange exchange) {
        return BindingBuilder.bind(sessionAttendanceCommandQueue)
                .to(exchange)
                .with(SESSION_ATTENDANCE_COMMAND_ROUTING_KEY);
    }

    @Bean
    Queue sessionAttendanceCommandDeadLetterQueue() {
        return QueueBuilder.durable(SESSION_ATTENDANCE_COMMAND_DLQ).build();
    }

    @Bean
    Binding sessionAttendanceCommandDeadLetterBinding(Queue sessionAttendanceCommandDeadLetterQueue,
                                                      DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(sessionAttendanceCommandDeadLetterQueue)
                .to(deadLetterExchange)
                .with(SESSION_ATTENDANCE_COMMAND_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter);
        return rabbitTemplate;
    }

    @Bean
    public ErrorHandler customRabbitErrorHandler() {
        return new ConditionalRejectingErrorHandler();
    }

    @Bean
    public RabbitListenerContainerFactory<SimpleMessageListenerContainer> rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter jsonMessageConverter,
            ErrorHandler customRabbitErrorHandler
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        factory.setErrorHandler(customRabbitErrorHandler);
        factory.setDefaultRequeueRejected(false);
        factory.setMissingQueuesFatal(true);
        return factory;
    }
}
