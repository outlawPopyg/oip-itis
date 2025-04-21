package com.ios.icl;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.apache.commons.math3.util.FastMath;

public class TFIDFCalculator {

    private static final String INPUT_DIR = "выкачка";
    private static final String OUTPUT_DIR = "tfidf_results";
    private static final String TOKENS_PREFIX = "tokens";
    private static final String LEMMAS_PREFIX = "lemmas";

    private static Map<String, Integer> tokenDocFrequency = new HashMap<>();
    private static Map<String, Integer> lemmaDocFrequency = new HashMap<>();
    private static int totalDocuments = 0;

    public static void main(String[] args) throws IOException {
        Files.createDirectories(Paths.get(OUTPUT_DIR));
        collectDocumentFrequencies();
        processDocuments();
    }

    private static void collectDocumentFrequencies() throws IOException {

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(INPUT_DIR), 
            path -> path.getFileName().toString().startsWith(TOKENS_PREFIX) && path.toString().endsWith(".txt"))) {
            for (Path tokenFile : stream) {
                totalDocuments++;
                Set<String> docTokens = new HashSet<>();
                Files.lines(tokenFile).forEach(line -> docTokens.add(line.trim()));
                docTokens.forEach(token -> tokenDocFrequency.merge(token, 1, Integer::sum));
            }
        }


        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(INPUT_DIR), 
            path -> path.getFileName().toString().startsWith(LEMMAS_PREFIX) && path.toString().endsWith(".txt"))) {
            for (Path lemmaFile : stream) {
                Set<String> docLemmas = new HashSet<>();
                Files.lines(lemmaFile).forEach(line -> {
                    String[] parts = line.split(" ");
                    if (parts.length > 0) docLemmas.add(parts[0].trim());
                });
                docLemmas.forEach(lemma -> lemmaDocFrequency.merge(lemma, 1, Integer::sum));
            }
        }
    }

    // Обработка документов для расчёта TF-IDF
    private static void processDocuments() throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(INPUT_DIR), 
            path -> path.getFileName().toString().startsWith(TOKENS_PREFIX) && path.toString().endsWith(".txt"))) {
            for (Path tokenFile : stream) {
                String docId = tokenFile.getFileName().toString()
                    .replace(TOKENS_PREFIX, "")
                    .replace(".txt", "");
                Path lemmaFile = tokenFile.resolveSibling(LEMMAS_PREFIX + docId + ".txt");

                // Частоты токенов
                Map<String, Integer> tokenFreq = new HashMap<>();
                int totalTokens = 0;
                List<String> tokens = Files.readAllLines(tokenFile);
                for (String token : tokens) {
                    token = token.trim();
                    tokenFreq.put(token, tokenFreq.getOrDefault(token, 0) + 1);
                    totalTokens++;
                }

                // TF-IDF для токенов
                List<String> tokenResults = new ArrayList<>();
                for (Map.Entry<String, Integer> entry : tokenFreq.entrySet()) {
                    String token = entry.getKey();
                    double tf = (double) entry.getValue() / totalTokens;
                    int df = tokenDocFrequency.getOrDefault(token, 1);
                    double idf = FastMath.log((double) totalDocuments / df);
                    double tfidf = tf * idf;
                    tokenResults.add(String.format("%s %.4f %.4f", token, idf, tfidf));
                }

                Path outTokenFile = Paths.get(OUTPUT_DIR, "tfidf_tokens" + docId + ".txt");
                Files.write(outTokenFile, tokenResults);

                // Обработка лемм
                if (Files.exists(lemmaFile)) {
                    Map<String, Double> lemmaTf = new HashMap<>();
                    List<String> lemmaLines = Files.readAllLines(lemmaFile);
                    for (String line : lemmaLines) {
                        String[] parts = line.split(" ");
                        if (parts.length < 1) continue;
                        String lemma = parts[0].trim();
                        int sum = 0;
                        for (int i = 1; i < parts.length; i++) {
                            sum += tokenFreq.getOrDefault(parts[i].trim(), 0);
                        }
                        lemmaTf.put(lemma, (double) sum / totalTokens);
                    }

                    // TF-IDF для лемм
                    List<String> lemmaResults = new ArrayList<>();
                    for (Map.Entry<String, Double> entry : lemmaTf.entrySet()) {
                        String lemma = entry.getKey();
                        double tf = entry.getValue();
                        int df = lemmaDocFrequency.getOrDefault(lemma, 1);
                        double idf = FastMath.log((double) totalDocuments / df);
                        double tfidf = tf * idf;
                        lemmaResults.add(String.format("%s %.4f %.4f", lemma, idf, tfidf));
                    }

                    Path outLemmaFile = Paths.get(OUTPUT_DIR, "tfidf_lemmas" + docId + ".txt");
                    Files.write(outLemmaFile, lemmaResults);
                }
            }
        }
    }
}