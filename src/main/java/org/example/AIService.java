package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class AIService {
    private static final String API_URL = "https://api.deepseek.com/chat/completions";

    // 核心修改：从本地配置文件或环境变量读取 Key
    public static String getApiKey() {
        // 1. 优先读取本地 config.properties
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream("config.properties")) {
            props.load(in);
            String key = props.getProperty("DEEPSEEK_API_KEY");
            if (key != null && !key.trim().isEmpty()) {
                return key.trim();
            }
        } catch (Exception ignored) {}

        // 2. 读取环境变量
        String envKey = System.getenv("DEEPSEEK_API_KEY");
        if (envKey != null && !envKey.trim().isEmpty()) {
            return envKey.trim();
        }

        // 3. 都没有，返回 null 触发离线模式
        return null;
    }

    // 保存 Key 到本地文件
    public static void saveApiKey(String key) {
        Properties props = new Properties();
        props.setProperty("DEEPSEEK_API_KEY", key == null ? "" : key.trim());
        try (FileOutputStream out = new FileOutputStream("config.properties")) {
            props.store(out, "WordContext Configuration");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String generateExercise(List<Word> words, String type) {
        String apiKey = getApiKey();

        // 离线演示模式：如果没有配置，直接返回预设数据
        if (apiKey == null || apiKey.isEmpty()) {
            return getMockResult(type);
        }

        StringBuilder wordListStr = new StringBuilder();
        for (Word w : words) {
            wordListStr.append(w.english).append(" ");
        }

        String systemPrompt;
        if ("完形填空".equals(type)) {
            systemPrompt = "你是一个英语出题专家。请利用用户提供的词汇，生成一篇约150词的英语完形填空。\n" +
                    "要求：\n" +
                    "1. 短文正文中用 ___1___ 表示挖空。\n" +
                    "2. 每道题必须包含4个选项(A,B,C,D)，并标明正确答案和详细解析。\n" +
                    "3. 严格遵守以下格式，不要任何多余的话语，不要用Markdown代码块：\n" +
                    "PASSAGE:\n[这里是短文]\n" +
                    "QUESTIONS:\n" +
                    "1|A. 选项1|B. 选项2|C. 选项3|D. 选项4|A|这里是解析内容";
        } else {
            systemPrompt = "你是一个英语出题专家。请利用用户提供的词汇，生成一篇约150词的英语语法填空（单句填空或语篇填空均可）。\n" +
                    "要求：\n" +
                    "1. 短文正文中用 ___1___ 表示挖空（考查词性转换、时态、语态等）。\n" +
                    "2. 不提供选项，要求考生根据上下文自己填写单词的正确形式。\n" +
                    "3. 严格遵守以下格式，不要任何多余的话语，不要用Markdown代码块：\n" +
                    "PASSAGE:\n[这里是短文]\n" +
                    "QUESTIONS:\n" +
                    "1|正确答案|这里是解析内容\n" +
                    "2|正确答案|这里是解析内容";
        }

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build();

        ObjectMapper mapper = new ObjectMapper();
        String jsonBody = "";

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "deepseek-chat");

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", systemPrompt));
            messages.add(Map.of("role", "user", "content", "这是我今天背的单词：\n" + wordListStr.toString() + "\n请帮我生成。"));

            requestBody.put("messages", messages);
            jsonBody = mapper.writeValueAsString(requestBody);
        } catch (Exception e) {
            return getMockResult(type);
        }

        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(jsonBody, JSON);

        Request request = new Request.Builder()
                .url(API_URL)
                .header("Authorization", "Bearer " + apiKey)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String responseBody = response.body().string();
                JsonNode root = mapper.readTree(responseBody);
                return root.path("choices").get(0).path("message").path("content").asText();
            } else {
                return "请求失败，HTTP 状态码: " + response.code() + "。请检查API Key是否有效，或稍后再试。";
            }
        } catch (IOException e) {
            return "网络异常: " + e.getMessage();
        }
    }

    private static String getMockResult(String type) {
        if ("完形填空".equals(type)) {
            return "PASSAGE:\n" +
                    "Learning a new language is a challenging but rewarding experience. When you first start, you might feel completely __1__ by the sheer amount of vocabulary. However, it is important to stay __2__ and not give up. Many students find that they can __3__ their progress by practicing a little bit every day.\n" +
                    "QUESTIONS:\n" +
                    "1|A. ignored|B. ignored|C. ignored|D. ignored|D|离线演示题目：未检测到API密钥，此处仅展示界面效果。\n" +
                    "2|A. calm|B. calm|C. calm|D. calm|A|离线演示题目：保持冷静。\n" +
                    "3|A. upset|B. upset|C. upset|D. upset|C|离线演示题目：提高进步。";
        } else {
            return "PASSAGE:\n" +
                    "The __1__ of a strong support system cannot be overstated when studying abroad. When students face difficulties, having someone to talk to can help them stay __2__ and focused.\n" +
                    "QUESTIONS:\n" +
                    "1|importance|离线演示题目：未检测到API密钥。此处应填名词。\n" +
                    "2|calm|离线演示题目：保持冷静。";
        }
    }
}