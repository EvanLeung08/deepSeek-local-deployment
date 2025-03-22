package org.example;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import okhttp3.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class OllamaBenchmark {
    private static final String API_URL = "http://localhost:11434/api/generate";
    private static final String MODEL_NAME = "deepseek-r1:7b";
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(500, TimeUnit.SECONDS)
            .writeTimeout(500, TimeUnit.SECONDS)
            .readTimeout(500, TimeUnit.SECONDS)
            .build();
    private static final MetricRegistry metrics = new MetricRegistry();

    public static void main(String[] args) throws InterruptedException {
        // 初始化性能计时器
        Timer timer = metrics.timer("generate_latency");

        // 测试参数配置
        String prompt = "用Java实现快速排序算法";
        int testRuns = 5;
        boolean streamMode = false; // 是否启用流式响应
        int maxTokens = 1000;

        // 创建线程池
        ExecutorService executor = Executors.newFixedThreadPool(testRuns);
        CountDownLatch latch = new CountDownLatch(testRuns);

        // 执行性能测试
        for (int i = 0; i < testRuns; i++) {
            final int runIndex = i;
            executor.submit(() -> {
                try (Timer.Context context = timer.time()) {
                    String response = generateText(prompt, streamMode, maxTokens);
                    int inputTokens = countTokens(prompt);
                    int outputTokens = countTokens(response);
                    long duration = context.stop();

                    System.out.printf("请求 %d | 并发数: %d | 输入Token数: %d | 生成Token数: %d | 最大Token数: %d | 耗时: %dms | 速率: %.2f tokens/s%n",
                            runIndex + 1, testRuns, inputTokens, outputTokens, maxTokens, duration / 1_000_000,
                            outputTokens / (duration / 1_000_000_000.0));
                } catch (Exception e) {
                    System.err.println("请求失败: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        // 等待所有任务完成
        latch.await();
        executor.shutdown();

        // 输出统计结果
        System.out.println("\n=== 汇总统计 ===");
        System.out.println("平均延迟: " + timer.getSnapshot().getMean() + "ms");
        System.out.println("最大延迟: " + timer.getSnapshot().getMax() + "ms");
        System.out.println("最小延迟: " + timer.getSnapshot().getMin() + "ms");
        System.out.printf("平均Token率: %.2f tokens/s%n",
                timer.getSnapshot().getMean() > 0 ?
                        timer.getCount() * timer.getSnapshot().getMean() / timer.getSnapshot().getMean() : 0);
    }

    private static String generateText(String prompt, boolean stream, int maxTokens) throws IOException {
        // 构建请求体
        String json = String.format("""
            {
                "model": "%s",
                "prompt": "%s",
                "stream": %b,
                "options": {
                    "temperature": 0.1,
                    "max_tokens": %d
                }
            }
            """, MODEL_NAME, prompt.replace("\"", "\\\""), stream, maxTokens);

        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(API_URL)
                .post(body)
                .build();

        // 发送请求并处理响应
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("HTTP错误: " + response.code());
            }

            JsonNode root = mapper.readTree(response.body().string());
            return root.path("response").asText();
        }
    }

    // 简单Token计数器（实际应使用模型对应Tokenizer）
    private static int countTokens(String text) {
        // 中文按字分割，英文按空格分割（近似估算）
        return text.split("\\s+").length + text.replaceAll("\\s+", "").length();
    }
}