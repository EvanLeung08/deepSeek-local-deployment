package org.example;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.io.IOException;

public class OllamaClient {

    private static final String OLLAMA_API_URL = "http://localhost:11434/api/generate";
    private static final ObjectMapper mapper = new ObjectMapper();

    public static String generateResponse(String prompt) throws IOException {
        // 构建请求体
        RequestBody body = buildRequestBody(prompt);
        
        // 发送请求
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(OLLAMA_API_URL)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code: " + response);

            // 解析流式响应（Ollama API返回逐块数据）
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = response.body().source().readUtf8Line()) != null) {
                JsonNode node = mapper.readTree(line);
                if (node.has("response")) {
                    result.append(node.get("response").asText());
                }
            }
            return result.toString();
        }
    }

    private static RequestBody buildRequestBody(String prompt) {
        // 构造JSON参数
        String json = String.format("""
        {
            "model": "deepseek-r1:7b",
            "prompt": "%s",
            "stream": true,
            "options": {
                "temperature": 0.7,
                "max_tokens": 1000
            }
        }
        """, prompt.replace("\"", "\\\""));  // 处理双引号转义

        return RequestBody.create(json, MediaType.parse("application/json"));
    }

    public static void main(String[] args) {
        try {
            String response = generateResponse("用Java写一个快速排序算法");
            System.out.println("AI响应：\n" + response);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}