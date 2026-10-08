package br.edu.trabalho05.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Delivery;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;

public abstract class ConsumerApplication {
    private static final ObjectMapper JSON = new ObjectMapper().findAndRegisterModules();
    private final String appName;
    private final String queue;
    private final long processingDelayMs;

    protected ConsumerApplication(String appName, String queue, long defaultDelayMs) {
        this.appName = appName;
        this.queue = queue;
        this.processingDelayMs = Long.parseLong(
                System.getenv().getOrDefault("PROCESSING_DELAY_MS", Long.toString(defaultDelayMs)));
    }

    protected abstract String classify(MessagePayload message);

    public final void run() throws Exception {
        Connection connection = RabbitSupport.connect(appName);
        Channel channel = connection.createChannel();
        RabbitSupport.declareTopology(channel);
        channel.basicQos(1);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                channel.close();
                connection.close();
            } catch (Exception exception) {
                System.err.printf("[%s] Falha ao encerrar conexão: %s%n",
                        appName, exception.getMessage());
            }
        }));

        channel.basicConsume(queue, false, (consumerTag, delivery) -> process(channel, delivery),
                consumerTag -> System.err.printf("[%s] Consumidor cancelado: %s%n",
                        appName, consumerTag));
        System.out.printf("[%s] Consumindo %s (delay=%dms)%n",
                appName, queue, processingDelayMs);
        new CountDownLatch(1).await();
    }

    private void process(Channel channel, Delivery delivery) {
        try {
            MessagePayload message = JSON.readValue(
                    delivery.getBody(), MessagePayload.class);
            if (message.content().contains("[FORCE_ERROR]")) {
                throw new IllegalStateException("Erro de processamento solicitado para demonstração");
            }
            String category = classify(message);
            Thread.sleep(processingDelayMs);
            System.out.printf("[%s] id=%s tipo=%s categoria=%s conteúdo=\"%s\"%n",
                    appName, message.id(), message.type(), category, message.content());
            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        } catch (Exception exception) {
            System.err.printf("[%s] Mensagem rejeitada e enviada à DLQ: %s%n",
                    appName, exception.getMessage());
            try {
                channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, false);
            } catch (Exception nackFailure) {
                System.err.printf("[%s] Falha ao rejeitar mensagem: %s%n",
                        appName, nackFailure.getMessage());
            }
        }
    }
}
