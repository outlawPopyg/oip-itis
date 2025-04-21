package com.ios.icl;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class IndexBuilder {
    private static final String INDEX_FILE = "inverted_index.txt";

    public static void main(String[] args) throws IOException {
        Map<String, Set<Integer>> index = new HashMap<>();
        buildIndex("выкачка", index);
        saveIndex(index);
    }

    private static void buildIndex(String dir, Map<String, Set<Integer>> index) throws IOException {
        Files.list(Paths.get(dir))
             .filter(p -> p.getFileName().toString().startsWith("lemmas"))
             .forEach(p -> processLemmaFile(p, index));
    }

    private static void processLemmaFile(Path file, Map<String, Set<Integer>> index) {
        try {
            int docId = extractDocId(file);
            Files.lines(file).forEach(line -> processIndexLine(line, docId, index));
        } catch (IOException e) {
            System.err.println("Error processing: " + file);
        }
    }

    private static int extractDocId(Path file) {
        String name = file.getFileName().toString();
        return Integer.parseInt(name.replace("lemmas", "").replace(".txt", ""));
    }

    private static void processIndexLine(String line, int docId, Map<String, Set<Integer>> index) {
        String[] parts = line.split(" ");
        if (parts.length < 1) return;
        
        String lemma = parts[0];
        index.computeIfAbsent(lemma, k -> new TreeSet<>()).add(docId);
    }

    private static void saveIndex(Map<String, Set<Integer>> index) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(INDEX_FILE))) {
            for (Map.Entry<String, Set<Integer>> entry : index.entrySet()) {
                writer.write(entry.getKey() + ":" + 
                    entry.getValue().stream()
                        .map(Object::toString)
                        .collect(Collectors.joining(",")) + "\n");
            }
        }
    }
}