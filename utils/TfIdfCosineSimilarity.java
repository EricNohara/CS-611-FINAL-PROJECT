package utils;

import java.util.*;

public class TfIdfCosineSimilarity implements SimilarityStrategy {
    @Override
    public double computeSimilarity(String doc1, String doc2) {
        List<String> tokens1 = tokenize(doc1);
        List<String> tokens2 = tokenize(doc2);

        Set<String> vocabulary = new HashSet<>();
        vocabulary.addAll(tokens1);
        vocabulary.addAll(tokens2);

        Map<String, Double> tfidf1 = computeTfIdf(tokens1, tokens2, vocabulary);
        Map<String, Double> tfidf2 = computeTfIdf(tokens2, tokens1, vocabulary);

        return cosineSimilarity(tfidf1, tfidf2);
    }

    private List<String> tokenize(String text) {
        return Arrays.asList(text.toLowerCase().split("\\W+"));
    }

    private Map<String, Double> computeTf(List<String> tokens, Set<String> vocabulary) {
        Map<String, Double> tf = new HashMap<>();
        for (String term : vocabulary) {
            long count = tokens.stream().filter(t -> t.equals(term)).count();
            tf.put(term, count / (double) tokens.size());
        }
        return tf;
    }

    private Map<String, Double> computeTfIdf(List<String> docTokens, List<String> otherDocTokens, Set<String> vocab) {
        Map<String, Double> tf = computeTf(docTokens, vocab);
        Map<String, Double> idf = new HashMap<>();
        int totalDocs = 2;

        for (String term : vocab) {
            int docsWithTerm = 0;
            if (docTokens.contains(term)) docsWithTerm++;
            if (otherDocTokens.contains(term)) docsWithTerm++;
            idf.put(term, Math.log((double) totalDocs / (1 + docsWithTerm)));
        }

        Map<String, Double> tfidf = new HashMap<>();
        for (String term : vocab) {
            tfidf.put(term, tf.get(term) * idf.get(term));
        }
        return tfidf;
    }

    private double cosineSimilarity(Map<String, Double> vec1, Map<String, Double> vec2) {
        double dot = 0.0;
        double mag1 = 0.0;
        double mag2 = 0.0;

        for (String term : vec1.keySet()) {
            double v1 = vec1.getOrDefault(term, 0.0);
            double v2 = vec2.getOrDefault(term, 0.0);
            dot += v1 * v2;
            mag1 += v1 * v1;
            mag2 += v2 * v2;
        }
        
        return mag1 == 0 || mag2 == 0 ? 0.0 : dot / (Math.sqrt(mag1) * Math.sqrt(mag2));
    }
}
