package br.edu.trabalho05.support;

import br.edu.trabalho05.common.ConsumerApplication;
import br.edu.trabalho05.common.MessagePayload;
import br.edu.trabalho05.common.NaiveBayesClassifier;
import br.edu.trabalho05.common.RabbitSettings;

import java.util.List;

public final class SupportConsumer extends ConsumerApplication {
    private final NaiveBayesClassifier model = new NaiveBayesClassifier(List.of(
            example("acessar login conta senha entrar bloqueado autenticação", "acesso"),
            example("não consigo entrar recuperar senha usuário", "acesso"),
            example("lento demora travando desempenho performance carregamento", "desempenho"),
            example("aplicação lenta resposta demorada sistema", "desempenho"),
            example("conexão conectar rede servidor cai desconecta", "conexão"),
            example("falha de comunicação indisponível conectar serviço", "conexão")
    ));

    public SupportConsumer() {
        super("support-consumer", RabbitSettings.SUPPORT_QUEUE, 1200);
    }

    @Override
    protected String classify(MessagePayload message) {
        return model.classify(message.content());
    }

    private static NaiveBayesClassifier.TrainingExample example(String text, String label) {
        return new NaiveBayesClassifier.TrainingExample(text, label);
    }

    public static void main(String[] args) throws Exception {
        new SupportConsumer().run();
    }
}
