package br.edu.trabalho05.sentiment;

import br.edu.trabalho05.common.ConsumerApplication;
import br.edu.trabalho05.common.MessagePayload;
import br.edu.trabalho05.common.NaiveBayesClassifier;
import br.edu.trabalho05.common.RabbitSettings;

import java.util.List;

public final class SentimentConsumer extends ConsumerApplication {
    private final NaiveBayesClassifier model = new NaiveBayesClassifier(List.of(
            example("excelente muito bom ótimo adorei satisfeito recomendo", "positivo"),
            example("qualidade ótima funciona bem fácil de usar", "positivo"),
            example("razoável mediano normal poderia ser melhor", "neutro"),
            example("nada especial experiência comum", "neutro"),
            example("ruim péssimo não gostei decepcionado horrível", "negativo"),
            example("parou de funcionar qualidade ruim insatisfeito", "negativo")
    ));

    public SentimentConsumer() {
        super("sentiment-consumer", RabbitSettings.REVIEW_QUEUE, 1200);
    }

    @Override
    protected String classify(MessagePayload message) {
        return model.classify(message.content());
    }

    private static NaiveBayesClassifier.TrainingExample example(String text, String label) {
        return new NaiveBayesClassifier.TrainingExample(text, label);
    }

    public static void main(String[] args) throws Exception {
        new SentimentConsumer().run();
    }
}
