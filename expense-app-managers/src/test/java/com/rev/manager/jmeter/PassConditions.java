package com.rev.manager.jmeter;

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.nio.file.Path;
import java.nio.file.Files;
import java.io.BufferedReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import java.util.*;

public class PassConditions {

    private static long percentile95;
    private static List<Long> responseTimes;    
    private static int failedRequests;
    private static int totalRequests;
    private static double averageResponseTime;
    private static double errorRate;    
    private static double throughput;    
    private static long maximumResponseTime;

    @BeforeAll
    static void loadPerformanceResults() throws Exception {

        Path resultsFile = Path.of("targetJmeter", "performance-results.jtl");

        responseTimes = new ArrayList<>();
        totalRequests = 0;
        failedRequests = 0;

        long firstTimestamp = Long.MAX_VALUE;
        long lastTimestamp = Long.MIN_VALUE;

        try (BufferedReader reader = Files.newBufferedReader(resultsFile)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                String[] fields = line.split(",", -1);
                if (fields.length < 8) {
                    continue;
                }
                try {
                    long timestamp = Long.parseLong(fields[0]);
                    long elapsed = Long.parseLong(fields[1]);
                    boolean success = Boolean.parseBoolean(fields[7]);
                    totalRequests++;
                    responseTimes.add(elapsed);
                    if (!success) {
                        failedRequests++;
                    }
                    firstTimestamp = Math.min(firstTimestamp, timestamp);
                    lastTimestamp = Math.max(lastTimestamp, timestamp);
                } catch (NumberFormatException e) {
                    // Skip header or malformed rows
                }
            }
        }

        assertTrue(
                totalRequests > 0,
                "No JMeter results were found."
        );

        errorRate =
                ((double) failedRequests / totalRequests) * 100;

        averageResponseTime =
                responseTimes.stream()
                        .mapToLong(Long::longValue)
                        .average()
                        .orElse(0);

        maximumResponseTime =
                Collections.max(responseTimes);

        Collections.sort(responseTimes);

        int percentileIndex =
                (int) Math.ceil(
                        responseTimes.size() * 0.95
                ) - 1;

        percentile95 =
                responseTimes.get(
                        Math.max(percentileIndex, 0)
                );

        double durationSeconds =
                (lastTimestamp - firstTimestamp) / 1000.0;

        throughput =
                durationSeconds > 0
                        ? totalRequests / durationSeconds
                        : 0;

        System.out.println(
                "===== PERFORMANCE RESULTS ====="
        );

        System.out.println(
                "Total Requests: " + totalRequests
        );

        System.out.println(
                "Error Rate: " + errorRate + "%"
        );

        System.out.println(
                "Average Response Time: "
                        + averageResponseTime + " ms"
        );

        System.out.println(
                "95th Percentile: "
                        + percentile95 + " ms"
        );

        System.out.println(
                "Maximum Response Time: "
                        + maximumResponseTime + " ms"
        );

        System.out.println(
                "Throughput: "
                        + throughput + " requests/sec"
        );
    }


    @Test
    void totalRequestsRequirement() {

        assertTrue(
                totalRequests > 0,
                "No requests were recorded."
        );
    }


    @Test
    void errorRateRequirement() {

        assertTrue(
                errorRate <= 15.0,
                "Error rate exceeded 15%. Actual: "
                        + errorRate + "%"
        );
    }


    @Test
    void averageResponseTimeRequirement() {

        assertTrue(
                averageResponseTime <= 600,
                "Average response time exceeded 600 ms. "
                        + "Actual: "
                        + averageResponseTime + " ms"
        );
    }


    @Test
    void percentile95Requirement() {

        assertTrue(
                percentile95 <= 1100,
                "95th percentile exceeded 1100 ms. "
                        + "Actual: "
                        + percentile95 + " ms"
        );
    }


    @Test
    void maximumResponseTimeRequirement() {

        assertTrue(
                maximumResponseTime <= 10000,
                "Maximum response time exceeded 10000 ms. "
                        + "Actual: "
                        + maximumResponseTime + " ms"
        );
    }


    @Test
    void throughputRequirement() {

        assertTrue(
                throughput >= 45,
                "Throughput was below 45 requests/sec. "
                        + "Actual: "
                        + throughput + " requests/sec"
        );
    }
}