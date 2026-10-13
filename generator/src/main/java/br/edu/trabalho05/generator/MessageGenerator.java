package br.edu.trabalho05.generator;

import br.edu.trabalho05.common.MessagePayload;
import br.edu.trabalho05.common.RabbitSettings;
import br.edu.trabalho05.common.RabbitSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class MessageGenerator {
    private static final ObjectMapper JSON = new ObjectMapper().findAndRegisterModules();
    private static final List<String> REVIEWS = List.of(
            "O produto é excelente e muito fácil de usar",
            "A qualidade é ótima, estou muito satisfeito",
            "O produto funciona bem e chegou no prazo",
            "A experiência foi razoável, nada de especial",
            "O produto é mediano e poderia ser melhor",
            "Não gostei, a qualidade é ruim",
            "O produto parou de funcionar e estou decepcionado");

    private static final List<String> SUPPORT_REQUESTS = List.of(
            "Não consigo acessar minha conta, a senha não funciona",
            "Meu login foi bloqueado e não consigo entrar",
            "O sistema está muito lento e demora para carregar",
            "A aplicação está travando e com baixo desempenho",
            "A conexão com o servidor cai toda hora",
            "Não consigo conectar ao serviço pela rede");

    private MessageGenerator() {}

    public static void main(String[] args) throws Exception {
        long failureEvery = Long.parseLong(
                System.getenv().getOrDefault("SIMULATE_FAILURE_EVERY", "0"));
        try (Connection connection = RabbitSupport.connect("message-generator");
             Channel channel = connection.createChannel()) {
            RabbitSupport.declareTopology(channel);
            channel.confirmSelect();
            long sequence = 0;
            while (!Thread.currentThread().isInterrupted()) {
                sequence++;
                boolean review = ThreadLocalRandom.current().nextBoolean();
                String type = review ? RabbitSettings.REVIEW_ROUTING_KEY
                        : RabbitSettings.SUPPORT_ROUTING_KEY;
                List<String> options = review ? REVIEWS : SUPPORT_REQUESTS;
                String content = options.get(ThreadLocalRandom.current().nextInt(options.size()));
                if (failureEvery > 0 && sequence % failureEvery == 0) {
                    content += " [FORCE_ERROR]";
                }

                MessagePayload message = new MessagePayload(UUID.randomUUID(), Instant.now(), type, content);
                
                byte[] body = JSON.writeValueAsBytes(message);

                AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                        .contentType("application/json")
                        .deliveryMode(2)
                        .messageId(message.id().toString())
                        .timestamp(new java.util.Date())
                        .build();

                
                channel.basicPublish(RabbitSettings.EXCHANGE, type, properties, body);

                channel.waitForConfirmsOrDie(5000);
                System.out.printf("[generator] #%d id=%s rota=%s%n",sequence, message.id(), type);
                
                Thread.sleep(150);
            }
        }
    }
}
