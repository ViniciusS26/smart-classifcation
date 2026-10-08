package br.edu.trabalho05.common;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public final class NaiveBayesClassifier {
    private static final Pattern TOKEN_PATTERN = Pattern.compile("[\\p{L}\\p{N}]+");

    private final Map<String, Integer> documentCounts = new HashMap<>();
    private final Map<String, Map<String, Integer>> tokenCounts = new HashMap<>();
    private final Map<String, Integer> tokenTotals = new HashMap<>();
    private final Map<String, Boolean> vocabulary = new HashMap<>();
    private int totalDocuments;

    public NaiveBayesClassifier(List<TrainingExample> examples) {
        if (examples.isEmpty()) {
            throw new IllegalArgumentException("O modelo precisa de exemplos de treinamento");
        }
        for (TrainingExample example : examples) {
            train(example.text(), example.label());
        }
    }

    public String classify(String text) {
        List<String> tokens = tokens(text);
        if (tokens.isEmpty()) {
            throw new IllegalArgumentException("Não há palavras para classificar");
        }

        String bestLabel = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        int vocabularySize = vocabulary.size();
        for (String label : documentCounts.keySet()) {
            double score = Math.log((double) documentCounts.get(label) / totalDocuments);
            int total = tokenTotals.get(label);
            Map<String, Integer> counts = tokenCounts.get(label);
            for (String token : tokens) {
                score += Math.log((counts.getOrDefault(token, 0) + 1.0)
                        / (total + vocabularySize));
            }
            if (score > bestScore) {
                bestScore = score;
                bestLabel = label;
            }
        }
        return bestLabel;
    }

    private void train(String text, String label) {
        documentCounts.merge(label, 1, Integer::sum);
        Map<String, Integer> counts = tokenCounts.computeIfAbsent(label, ignored -> new HashMap<>());
        for (String token : tokens(text)) {
            vocabulary.put(token, true);
            counts.merge(token, 1, Integer::sum);
            tokenTotals.merge(label, 1, Integer::sum);
        }
        totalDocuments++;
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
