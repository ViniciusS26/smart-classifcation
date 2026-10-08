package br.edu.trabalho05.common;

public final class RabbitSettings {
    public static final String EXCHANGE = "messages.topic";
    public static final String DEAD_LETTER_EXCHANGE = "messages.dlx";
    public static final String REVIEW_ROUTING_KEY = "review.product";
    public static final String SUPPORT_ROUTING_KEY = "support.request";
    public static final String REVIEW_QUEUE = "reviews.queue";
    public static final String SUPPORT_QUEUE = "support.queue";
    public static final String REVIEW_DLQ = "reviews.dlq";
    public static final String SUPPORT_DLQ = "support.dlq";
    public static final String REVIEW_DEAD_ROUTING_KEY = "dead.review";
    public static final String SUPPORT_DEAD_ROUTING_KEY = "dead.support";

    private RabbitSettings() {}

    public static String host() {
        return System.getenv().getOrDefault("RABBITMQ_HOST", "localhost");
    }

    public static int port() {
        return Integer.parseInt(System.getenv().getOrDefault("RABBITMQ_PORT", "5672"));
    }

    public static String username() {
        return System.getenv().getOrDefault("RABBITMQ_USER", "guest");
    }

    public static String password() {
        return System.getenv().getOrDefault("RABBITMQ_PASSWORD", "guest");
    }
}
