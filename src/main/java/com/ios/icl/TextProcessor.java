package com.ios.icl;

import edu.stanford.nlp.util.CoreMap;

import java.util.*;
import java.io.*;
import java.nio.file.*;

import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.ling.*;

public class TextProcessor {

    private static StanfordCoreNLP pipeline;
    private static Set<String> stopWords = new HashSet<>(Arrays.asList(
            "a", "an", "the", "and", "or", "but", "of", "at", "by", "for",
            "with", "about", "to", "from", "in", "on", "that", "as", "it",
            "is", "be", "are", "was", "were", "this", "which", "have", "has"
    ));

    public static void main(String[] args) throws Exception {
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma");
        pipeline = new StanfordCoreNLP(props);

        processFiles("выкачка");
    }

    private static void processFiles(String inputDir) throws Exception {
        Files.list(Paths.get(inputDir))
                .filter(Files::isRegularFile)
                .parallel()
                .forEach(file -> processFile(file));
    }

    private static void processFile(Path file) {
        try {
            String content = new String(Files.readAllBytes(file));
            Set<String> uniqueTokens = new TreeSet<>();
            Map<String, Set<String>> lemmaMap = new TreeMap<>();

            Annotation annotation = new Annotation(content);
            pipeline.annotate(annotation);

            for (CoreMap sentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
                for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                    String word = token.word().toLowerCase();
                    String lemma = token.lemma().toLowerCase();

                    if (isValidToken(word)) {
                        uniqueTokens.add(word);
                        lemmaMap.computeIfAbsent(lemma, k -> new TreeSet<>()).add(word);
                    }
                }
            }

            // Генерация имен выходных файлов
            String baseName = file.getFileName().toString().replaceFirst("[.][^.]+$", "");
            Path tokensFile = file.resolveSibling("tokens" + baseName + ".txt");
            Path lemmasFile = file.resolveSibling("lemmas" + baseName + ".txt");

            // Запись токенов
            try (BufferedWriter writer = Files.newBufferedWriter(tokensFile)) {
                for (String token : uniqueTokens) {
                    writer.write(token + "\n");
                }
            }

            // Запись лемм
            try (BufferedWriter writer = Files.newBufferedWriter(lemmasFile)) {
                for (Map.Entry<String, Set<String>> entry : lemmaMap.entrySet()) {
                    writer.write(entry.getKey() + " " + String.join(" ", entry.getValue()) + "\n");
                }
            }

            System.out.println("Processed: " + file.getFileName());
        } catch (Exception e) {
            System.err.println("Error processing " + file.getFileName() + ": " + e.getMessage());
        }
    }

    private static boolean isValidToken(String token) {
        return !stopWords.contains(token) &&
                !token.matches(".*\\d.*") &&
                token.matches("^[a-zA-Z']+$") &&
                token.length() > 2;
    }
}