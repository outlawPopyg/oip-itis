package com.ios.icl;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

@Component
public class PageRankProcessor {
    public Map<Integer, PageRank> indexMap = new HashMap<>();
    public Map<Integer, Double> pageRanks = new HashMap<>();

    @PostConstruct
    public void init() {
        initRanks();
    }

    record OrderedFile(Path path, int index) {
    }

    /**
     * @param referencedDocs документы, ссылающиеся на pageUrl
     */
    public record PageRank(String pageUrl, List<Integer> referencedDocs) {
    }

    public void initRanks() {


        try (Stream<String> lines = Files.lines(Path.of("index.txt"))) {
            lines.forEach(line -> {
                String[] split = line.split(" ");
                indexMap.put(Integer.parseInt(split[0]), new PageRank(split[1], new ArrayList<>()));
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try (Stream<Path> pages = Files.list(Path.of("pages"))) {
            pages
                    .map(path -> new OrderedFile(path,
                            Integer.parseInt(path.getFileName().toString().replaceAll("[a-z.]", ""))))
                    .parallel()
                    .forEach(file -> {
                        String resourceName = URI.create(indexMap.get(file.index).pageUrl()).getPath();
                        try {
                            Files.list(Path.of("pages")).filter(path ->
                                            Integer.parseInt(path.getFileName().toString().replaceAll("[a-z.]", "")) != file.index)
                                    .parallel()
                                    .forEach(f -> {
                                        try {
                                            Files.lines(f)
                                                    .filter(l -> l.contains(String.format("<a href=\"%s\"", resourceName)))
                                                    .findAny()
                                                    .ifPresent(l -> {
                                                        int index = Integer.parseInt(f.getFileName().toString().replaceAll("[a-z.]", ""));
                                                        indexMap.get(file.index).referencedDocs().add(index);
                                                    });

                                        } catch (IOException e) {
                                            throw new RuntimeException(e);
                                        }
                                    });
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            int i = 0;
        }

        double d = 0.85;
        double c = (1 - d) / 100;

        for (int i = 0; i < 100; i++) {
            for (Map.Entry<Integer, PageRank> indexMapEntry : indexMap.entrySet()) {

                double pageRank = indexMapEntry.getValue().referencedDocs().stream()
                        .filter(docId -> getPageOutgoingRefs(docId) != 0)
                        .mapToDouble(docId -> pageRanks.getOrDefault(docId, 0.01) / getPageOutgoingRefs(docId))
                        .sum();

                pageRanks.put(indexMapEntry.getKey(), pageRank);
            }
        }


        int i = 0;

    }

    private int getPageOutgoingRefs(int docId) {
        return (int) indexMap.values().stream()
                .map(PageRank::referencedDocs)
                .flatMap(Collection::stream)
                .filter(index -> docId == index)
                .count();
    }
}
