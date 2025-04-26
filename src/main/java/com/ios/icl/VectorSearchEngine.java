package com.ios.icl;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;


public class VectorSearchEngine {
    private static final String INPUT_DIR = "tfidf_tokens";
    private static final String TOKENS_TFIDF_PREFIX = "tfidf_tokens";
    private static final String LEMMAS_TFIDF_PREFIX = "tfidf_lemmas";
    private static final String LEMMAS_MAP_PREFIX = "lemmas";

    private final Map<String, Map<String, Double>> tokenVectors = new HashMap<>();
    private final Map<String, Map<String, Double>> lemmaVectors = new HashMap<>();
    private final Map<String, Double> tokenIdf = new HashMap<>();
    private final Map<String, Double> lemmaIdf = new HashMap<>();
    private final Map<String, List<String>> lemmaMap = new HashMap<>();
    private final int totalDocs;

    public VectorSearchEngine() throws IOException {
        loadLemmaMapping();
        this.totalDocs = loadTfIdfVectors(TOKENS_TFIDF_PREFIX, tokenVectors, tokenIdf);
        loadTfIdfVectors(LEMMAS_TFIDF_PREFIX, lemmaVectors, lemmaIdf);
    }


    private void loadLemmaMapping() throws IOException {
        Path dir = Paths.get(INPUT_DIR);
        try (var files = Files.list(dir)) {
            files.filter(p -> p.getFileName().toString().startsWith(LEMMAS_MAP_PREFIX)
                            && p.toString().endsWith(".txt"))
                    .forEach(path -> {
                        try (BufferedReader reader = Files.newBufferedReader(path)) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                String[] parts = line.strip().split("\\s+");
                                if (parts.length >= 2) {
                                    String token = parts[0].toLowerCase();
                                    List<String> lemmas = new ArrayList<>();
                                    for (int i = 1; i < parts.length; i++) {
                                        lemmas.add(parts[i].toLowerCase());
                                    }
                                    lemmaMap.put(token, lemmas);
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        }
    }

    private int loadTfIdfVectors(String prefix,
                                 Map<String, Map<String, Double>> docVectors,
                                 Map<String, Double> globalIdf) throws IOException {
        Path dir = Paths.get(INPUT_DIR);
        int docs = 0;
        try (var files = Files.list(dir)) {
            for (Path path : (Iterable<Path>) files::iterator) {
                String fileName = path.getFileName().toString();
                if (!fileName.startsWith(prefix) || !fileName.endsWith(".txt")) continue;
                String docId = fileName.substring(prefix.length(), fileName.length() - 4);
                Map<String, Double> vec = new HashMap<>();
                try (BufferedReader reader = Files.newBufferedReader(path)) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] parts = line.strip().split("\\s+");
                        if (parts.length >= 3) {
                            String term = parts[0].toLowerCase();
                            double idf = Double.parseDouble(parts[1]);
                            double tfidf = Double.parseDouble(parts[2]);
                            vec.put(term, tfidf);
                            globalIdf.putIfAbsent(term, idf);
                        }
                    }
                }
                docVectors.put(docId, vec);
                docs++;
            }
        }
        return docs;
    }


    private Map<String, Double> vectorizeQuery(String query, boolean useLemmas) {
        List<String> tokens = tokenize(query);
        Map<String, Integer> counts = new HashMap<>();
        for (String token : tokens) {
            String t = token.toLowerCase();
            if (useLemmas) {
                List<String> lemmas = lemmaMap.getOrDefault(t, List.of(t));
                for (String lemma : lemmas) counts.merge(lemma, 1, Integer::sum);
            } else {
                counts.merge(t, 1, Integer::sum);
            }
        }
        Map<String, Double> qVec = new HashMap<>();
        Map<String, Double> idfMap = useLemmas ? lemmaIdf : tokenIdf;
        for (var entry : counts.entrySet()) {
            String term = entry.getKey();
            double tf = 1 + Math.log(entry.getValue());
            double idf = idfMap.getOrDefault(term, Math.log((double) (totalDocs + 1)) + 1);
            qVec.put(term, tf * idf);
        }
        return qVec;
    }


    private double cosineSimilarity(Map<String, Double> v1, Map<String, Double> v2) {
        double dot = 0, norm1 = 0, norm2 = 0;
        Set<String> terms = new HashSet<>(v1.keySet());
        terms.addAll(v2.keySet());
        for (String t : terms) {
            double a = v1.getOrDefault(t, 0.0);
            double b = v2.getOrDefault(t, 0.0);
            dot += a * b;
            norm1 += a * a;
            norm2 += b * b;
        }
        return dot / (Math.sqrt(norm1) * Math.sqrt(norm2) + 1e-10);
    }

    private List<String> tokenize(String text) {
        return Arrays.stream(text.split("\\W+"))
                .filter(s -> !s.isBlank())
                .toList();
    }

    private List<Result> searchInternal(String query, boolean useLemmas, int topK) {
        Map<String, Double> qVec = vectorizeQuery(query, useLemmas);
        Map<String, Map<String, Double>> docs = useLemmas ? lemmaVectors : tokenVectors;
        PriorityQueue<Result> pq = new PriorityQueue<>(Comparator.comparingDouble(r -> -r.score));
        for (var e : docs.entrySet()) {
            double score = cosineSimilarity(e.getValue(), qVec);
            pq.add(new Result(e.getKey(), score));
        }
        List<Result> out = new ArrayList<>();
        for (int i = 0; i < topK && !pq.isEmpty(); i++) out.add(pq.poll());
        return out;
    }


    public List<Result> searchByTokens(String query, int topK) {
        return searchInternal(query, false, topK);
    }

    public List<Result> searchByLemmas(String query, int topK) {
        return searchInternal(query, true, topK);
    }

    public List<Result> searchByBoth(String query, int topK) {
        var t = searchByTokens(query, topK);
        var l = searchByLemmas(query, topK);
        Map<String, Double> merged = new HashMap<>();
        t.forEach(r -> merged.put(r.docId, r.score / 2));
        l.forEach(r -> merged.merge(r.docId, r.score / 2, Double::sum));
        PriorityQueue<Result> pq = new PriorityQueue<>(Comparator.comparingDouble(r -> -r.score));
        merged.forEach((d, s) -> pq.add(new Result(d, s)));
        List<Result> res = new ArrayList<>();
        for (int i = 0; i < topK && !pq.isEmpty(); i++) res.add(pq.poll());
        return res;
    }

    public record Result(String docId, double score) {
    }

    public static void main(String[] args) throws IOException {
        PageRankProcessor pageRankProcessor = new PageRankProcessor();
        pageRankProcessor.initRanks();

        Scanner scanner = new Scanner(System.in);
        String query;
        VectorSearchEngine searchEngine = new VectorSearchEngine();
        System.out.println("Введите запрос (q для выхода)");
        System.out.print("Query> ");
        while (!(query = scanner.nextLine()).equals("q")) {
            System.out.println("Results for '" + query + "':");
            searchEngine.searchByTokens(query, 10).stream()
                    .sorted(Comparator.comparing(Result::score, Comparator.reverseOrder())
                            .thenComparing(r -> pageRankProcessor.indexMap.get(Integer.parseInt(r.docId())).pageRank(), Comparator.reverseOrder()))
                    .forEach(r -> System.out.printf("%s (%.4f)%n", r.docId, r.score));
            System.out.print("Query> ");
        }
    }
}
