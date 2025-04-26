package com.ios.icl;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

@Component
public class PageRankProcessor {
    public Map<Integer, PageRank> indexMap = new HashMap<>();

    @PostConstruct
    public void init() {
        initRanks();
    }

    record OrderedFile(Path path, int index) {
    }

    public static final class PageRank {
        private final String pageUrl;
        private int pageRank;

        PageRank(String pageUrl, int pageRank) {
            this.pageUrl = pageUrl;
            this.pageRank = pageRank;
        }

        public String pageUrl() {
            return pageUrl;
        }

        public int pageRank() {
            return pageRank;
        }

        public void setRank(int rank) {
            this.pageRank = rank;
        }
    }

    public void initRanks() {


        try (Stream<String> lines = Files.lines(Path.of("index.txt"))) {
            lines.forEach(line -> {
                String[] split = line.split(" ");
                indexMap.put(Integer.parseInt(split[0]), new PageRank(split[1], 0));
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
                                            long count = Files.lines(f)
                                                    .filter(l -> l.contains(String.format("<a href=\"%s\"", resourceName)))
                                                    .count();

                                            if (count > 0) {
                                                System.out.println("In " + f.getFileName().toString() + " founded " +
                                                        count + " refs to " + resourceName);
                                            }

                                            indexMap.get(file.index).setRank((int) (indexMap.get(file.index).pageRank() + count));
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
        }
    }
}
