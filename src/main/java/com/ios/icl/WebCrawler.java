package com.ios.icl;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class WebCrawler {

    public static void main(String[] args) {
        List<String> urls = readUrlsFromFile("urls.txt");
        if (urls == null) {
            System.err.println("Не удалось прочитать URL из файла.");
            return;
        }

        String outputDir = "выкачка";
        createDirectory(outputDir);

        String indexFile = "index.txt";
        int count = 0;

        try (BufferedWriter indexWriter = new BufferedWriter(new FileWriter(indexFile))) {
            for (String urlStr : urls) {
                if (count >= 100) break;

                try {
                    URL url = new URL(urlStr);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0");

                    String contentType = conn.getContentType();
                    if (contentType == null || !contentType.startsWith("text/html")) {
                        System.out.println("Пропускаем URL: " + urlStr);
                        conn.disconnect();
                        continue;
                    }

                    try (InputStream inputStream = conn.getInputStream()) {
                        byte[] contentBytes = readAllBytes(inputStream);
                        String fileName = (count + 1) + ".txt";
                        Files.write(Paths.get(outputDir, fileName), contentBytes);

                        indexWriter.write((count + 1) + " " + urlStr + "\n");
                        count++;
                        System.out.println("Скачано: " + fileName);
                    }
                    conn.disconnect();
                } catch (Exception e) {
                    System.err.println("Ошибка при обработке " + urlStr + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Ошибка записи в index.txt: " + e.getMessage());
        }

        System.out.println("Готово. Скачано страниц: " + count);
    }

    private static List<String> readUrlsFromFile(String filename) {
        List<String> urls = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) urls.add(line);
            }
        } catch (IOException e) {
            System.err.println("Ошибка чтения " + filename + ": " + e.getMessage());
            return null;
        }
        return urls;
    }

    private static void createDirectory(String dirName) {
        File dir = new File(dirName);
        if (!dir.exists() && !dir.mkdir()) {
            System.err.println("Не удалось создать директорию: " + dirName);
        }
    }

    private static byte[] readAllBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[4096];
        int bytesRead;
        while ((bytesRead = inputStream.read(data)) != -1) {
            buffer.write(data, 0, bytesRead);
        }
        return buffer.toByteArray();
    }
}