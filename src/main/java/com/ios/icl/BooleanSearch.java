package com.ios.icl;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class BooleanSearch {
    private static final String INDEX_FILE = "inverted_index.txt";
    private final Map<String, Set<Integer>> index = new HashMap<>();
    private final Set<Integer> allDocs = new HashSet<>();

    public static void main(String[] args) throws IOException {
        BooleanSearch searcher = new BooleanSearch();
        searcher.loadIndex();
        searcher.runConsole();
    }

    private void loadIndex() throws IOException {
        Files.lines(Paths.get(INDEX_FILE)).forEach(line -> {
            String[] parts = line.split(":");
            if (parts.length != 2) return;

            String term = parts[0];
            Set<Integer> docs = Arrays.stream(parts[1].split(","))
                .map(Integer::parseInt)
                .collect(Collectors.toSet());

            index.put(term, docs);
            allDocs.addAll(docs);
        });
    }

    private void runConsole() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Boolean Search (AND/OR/NOT, use parentheses)");

        while (true) {
            System.out.print("Query> ");
            String query = scanner.nextLine().trim();
            
            if (query.equalsIgnoreCase("exit")) break;
            
            try {
                Set<Integer> result = evaluate(query);
                System.out.println("Results: " + result.stream()
                    .sorted()
                    .map(Object::toString)
                    .collect(Collectors.joining(", ")));
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
        scanner.close();
    }

    private Set<Integer> evaluate(String query) {
        return parseExpression(query.toLowerCase()
            .replaceAll("\\s+", " ")
            .replace("(", " ( ")
            .replace(")", " ) "));
    }

    private Set<Integer> parseExpression(String expr) {
        Stack<Set<Integer>> stack = new Stack<>();
        Stack<String> ops = new Stack<>();
        
        for (String token : expr.split("\\s+")) {
            if (token.isEmpty()) continue;
            
            switch (token) {
                case "(":
                    ops.push(token);
                    break;
                case ")":
                    while (!ops.peek().equals("(")) 
                        stack.push(applyOp(ops.pop(), stack.pop(), stack.pop()));
                    ops.pop();
                    break;
                case "and":
                case "or":
                case "not":
                    while (!ops.isEmpty() && precedence(ops.peek()) >= precedence(token)) 
                        stack.push(applyOp(ops.pop(), stack.pop(), stack.pop()));
                    ops.push(token);
                    break;
                default:
                    stack.push(getDocsForTerm(token));
            }
        }

        while (!ops.isEmpty()) 
            stack.push(applyOp(ops.pop(), stack.pop(), stack.pop()));

        return stack.pop();
    }

    private Set<Integer> applyOp(String op, Set<Integer> b, Set<Integer> a) {
        switch (op) {
            case "and": return intersection(a, b);
            case "or": return union(a, b);
            case "not": return difference(a, b);
            default: throw new IllegalArgumentException("Unknown operator: " + op);
        }
    }

    private Set<Integer> getDocsForTerm(String term) {
        if (term.startsWith("not")) {
            String cleanTerm = term.substring(3);
            return difference(allDocs, index.getOrDefault(cleanTerm, Collections.emptySet()));
        }
        return index.getOrDefault(term, Collections.emptySet());
    }

    private int precedence(String op) {
        switch (op) {
            case "not": return 3;
            case "and": return 2;
            case "or": return 1;
            default: return 0;
        }
    }

    private Set<Integer> union(Set<Integer> a, Set<Integer> b) {
        Set<Integer> result = new HashSet<>(a);
        result.addAll(b);
        return result;
    }

    private Set<Integer> intersection(Set<Integer> a, Set<Integer> b) {
        Set<Integer> result = new HashSet<>(a);
        result.retainAll(b);
        return result;
    }

    private Set<Integer> difference(Set<Integer> a, Set<Integer> b) {
        Set<Integer> result = new HashSet<>(a);
        result.removeAll(b);
        return result;
    }
}