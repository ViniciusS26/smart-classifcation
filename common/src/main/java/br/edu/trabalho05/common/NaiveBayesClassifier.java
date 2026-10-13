package br.edu.trabalho05.common;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import smile.classification.DiscreteNaiveBayes;

public final class NaiveBayesClassifier {
    private static final Pattern TOKEN_PATTERN = Pattern.compile("[\\p{L}\\p{N}]+");

    private final Map<String, Integer> vocabulary;
    private final List<String> labels;
    private final DiscreteNaiveBayes model;

    public NaiveBayesClassifier(List<TrainingExample> examples) {
        if (examples.isEmpty()) {
            throw new IllegalArgumentException("O modelo precisa de exemplos de treinamento");
        }

        vocabulary = new LinkedHashMap<>();
        Map<String, Integer> labelIndexes = new LinkedHashMap<>();
        for (TrainingExample example : examples) {
            labelIndexes.computeIfAbsent(example.label(), ignored -> labelIndexes.size());
            for (String token : tokens(example.text())) {
                vocabulary.computeIfAbsent(token, ignored -> vocabulary.size());
            }
        }
        if (vocabulary.isEmpty()) {
            throw new IllegalArgumentException("Os exemplos de treinamento precisam conter palavras");
        }

        labels = new ArrayList<>(labelIndexes.keySet());
        model = new DiscreteNaiveBayes(
                DiscreteNaiveBayes.Model.MULTINOMIAL, labels.size(), vocabulary.size());
        for (TrainingExample example : examples) {
            model.update(vectorize(example.text()), labelIndexes.get(example.label()));
        }
    }

    public String classify(String text) {
        List<String> tokens = tokens(text);
        if (tokens.isEmpty()) {
            throw new IllegalArgumentException("Não há palavras para classificar");
        }

        return labels.get(model.predict(vectorize(tokens)));
    }

    private int[] vectorize(String text) {
        return vectorize(tokens(text));
    }

    private int[] vectorize(List<String> tokens) {
        int[] vector = new int[vocabulary.size()];
        for (String token : tokens) {
            Integer index = vocabulary.get(token);
            if (index != null) {
                vector[index]++;
            }
        }
        return vector;
    }

    private static List<String> tokens(String text) {
        String normalized = Normalizer.normalize(text.toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return TOKEN_PATTERN.matcher(normalized).results()
                .map(match -> match.group())
                .toList();
    }

    public record TrainingExample(String text, String label) {}
}
