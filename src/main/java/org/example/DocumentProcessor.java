package org.example;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class DocumentProcessor {
    // 存储文档片段和对应的向量
    static Map<String, List<Double>> documentEmbeddings = new HashMap<>();

    public static void main(String[] args) throws IOException {
        String apiKey = "tid=38a5c8942744b2c86026e97db7b1c278;exp=1743346837;sku=yearly_subscriber;proxy-ep=proxy.individual.githubcopilot.com;st=dotcom;chat=1;cit=1;malfil=1;editor_preview_features=1;ccr=1;rt=1;8kp=1;ip=43.239.85.11;asn=AS63888:b26bb1b7be8014f6744bfd42dc13a3df6dec7938beeb3804616cde11a316c7b5";
        
        // 1. 读取你的文档文件（假设是文本文件）
        String docContent = Files.readString(Paths.get("your_document.txt"));
        
        // 2. 将文档拆分成段落（按换行符拆分，实际可按需求优化）
        String[] paragraphs = docContent.split("\n\n"); 

        // 3. 为每个段落生成嵌入向量
        for (String paragraph : paragraphs) {
            List<Double> embedding = getEmbedding(apiKey, paragraph);
            documentEmbeddings.put(paragraph, embedding);
        }
        System.out.println("文档预处理完成！");
    }

    // 调用 Embeddings API 获取向量
    public static List<Double> getEmbedding(String apiKey, String text) throws IOException {
        OkHttpClient client = new OkHttpClient();

        // Create JSON object using Gson
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("model", "copilot-text-embedding-ada-002");

        JsonArray inputArray = new JsonArray();
        inputArray.add(text);
        jsonObject.add("input", inputArray);

        String jsonBody = jsonObject.toString();
        RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url("https://api.githubcopilot.com/embeddings")
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("x-request-id", "5883a6a7-1995-4036-899d-b268ffbe2b26")
                .addHeader("vscode-sessionid", "731b4209-8f5d-4864-b56c-4bdf021fec011743340560435015")
                .addHeader("vscode-machineid", "")
                .addHeader("copilot-integration-id", "vscode-chat")
                .addHeader("openai-organization", "github-copilot")
                .addHeader("openai-intent", "conversation-panel")
                .addHeader("User-Agent", "github.com/stong1994/github-copilot-api/1.0.0")
                .addHeader("Client-Version", "1.0.0")
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body().string();
            System.out.println(responseBody);
            // Parse JSON to extract the embedding (simplified here)
            return parseEmbeddingFromJson(responseBody);
        }
    }

    // 简化的 JSON 解析（实际建议用 Gson/Jackson）
    private static List<Double> parseEmbeddingFromJson(String json) {
        List<Double> embedding = new ArrayList<>();
        JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
        JsonArray dataArray = jsonObject.getAsJsonArray("data");
        if (dataArray != null && dataArray.size() > 0) {
            JsonObject firstDataObject = dataArray.get(0).getAsJsonObject();
            JsonArray embeddingArray = firstDataObject.getAsJsonArray("embedding");
            for (JsonElement element : embeddingArray) {
                embedding.add(element.getAsDouble());
            }
        }
        return embedding;
    }
}