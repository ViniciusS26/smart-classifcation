package br.edu.trabalho05.common;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public final class RabbitSupport {
    private RabbitSupport() {}

    public static Connection connect(String clientName) throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(RabbitSettings.host());
        factory.setPort(RabbitSettings.port());
        factory.setUsername(RabbitSettings.username());
        factory.setPassword(RabbitSettings.password());
        factory.setConnectionTimeout(5000);

        IOException lastFailure = null;
        for (int attempt = 1; attempt <= 30; attempt++) {
            try {
                return factory.newConnection(clientName);
            } catch (IOException exception) {
                lastFailure = exception;
                System.err.printf("[%s] RabbitMQ indisponível (tentativa %d/30)%n",
                        clientName, attempt);
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Inicialização interrompida", interrupted);
                }
            }
        }
        throw new IOException("Não foi possível conectar ao RabbitMQ após 30 tentativas",
                lastFailure);
    }

    public static void declareTopology(Channel channel) throws IOException {
        channel.exchangeDeclare(RabbitSettings.EXCHANGE, "topic", true);
        channel.exchangeDeclare(RabbitSettings.DEAD_LETTER_EXCHANGE, "direct", true);

        declareQueue(channel, RabbitSettings.REVIEW_QUEUE, RabbitSettings.REVIEW_DEAD_ROUTING_KEY);
        declareQueue(channel, RabbitSettings.SUPPORT_QUEUE, RabbitSettings.SUPPORT_DEAD_ROUTING_KEY);
        channel.queueDeclare(RabbitSettings.REVIEW_DLQ, true, false, false, null);
        channel.queueDeclare(RabbitSettings.SUPPORT_DLQ, true, false, false, null);

        channel.queueBind(RabbitSettings.REVIEW_QUEUE, RabbitSettings.EXCHANGE,
                RabbitSettings.REVIEW_ROUTING_KEY);
        channel.queueBind(RabbitSettings.SUPPORT_QUEUE, RabbitSettings.EXCHANGE,
                RabbitSettings.SUPPORT_ROUTING_KEY);
        channel.queueBind(RabbitSettings.REVIEW_DLQ, RabbitSettings.DEAD_LETTER_EXCHANGE,
                RabbitSettings.REVIEW_DEAD_ROUTING_KEY);
        channel.queueBind(RabbitSettings.SUPPORT_DLQ, RabbitSettings.DEAD_LETTER_EXCHANGE,
                RabbitSettings.SUPPORT_DEAD_ROUTING_KEY);
    }

    private static void declareQueue(Channel channel, String queue, String deadRoutingKey)
            throws IOException {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("x-dead-letter-exchange", RabbitSettings.DEAD_LETTER_EXCHANGE);
        arguments.put("x-dead-letter-routing-key", deadRoutingKey);
        channel.queueDeclare(queue, true, false, false, arguments);
    }
}
