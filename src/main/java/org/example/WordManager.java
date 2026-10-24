package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WordManager {
    private List<Word> allWords = new ArrayList<>();
    private List<Word> todayNewWords = new ArrayList<>();
    private List<Word> weakWords = new ArrayList<>();

    public WordManager() {
        loadWordsFromJsonl();
    }

    private void loadWordsFromJsonl() {
        File file = new File("words.jsonl");
        if (!file.exists()) {
            System.err.println("未找到 words.jsonl 文件！");
            loadMockData();
            return;
        }

        ObjectMapper mapper = new ObjectMapper();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {

            String line;
            int count = 0;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                try {
                    JsonNode node = mapper.readTree(line);
                    String english = node.path("word").asText();

                    // 提取音标
                    String us = node.path("us").asText().trim();
                    String uk = node.path("uk").asText().trim();
                    String phonetic = "";

                    // 修改：使用 US /.../ | UK /.../ 的对称格式
                    if (!us.isEmpty() && !uk.isEmpty()) {
                        phonetic = "US /" + us + "/   |   UK /" + uk + "/";
                    } else if (!us.isEmpty()) {
                        phonetic = "US /" + us + "/";
                    } else if (!uk.isEmpty()) {
                        phonetic = "UK /" + uk + "/";
                    }

                    // 提取释义
                    StringBuilder meaningBuilder = new StringBuilder();
                    JsonNode translationsNode = node.path("translations");
                    if (translationsNode.isArray()) {
                        for (JsonNode t : translationsNode) {
                            String type = t.path("type").asText();
                            String translation = t.path("translation").asText();
                            if (!type.isEmpty()) {
                                meaningBuilder.append(type).append(". ");
                            }
                            meaningBuilder.append(translation).append("\n");
                        }
                    }
                    String meaning = meaningBuilder.toString().trim();

                    if (!english.isEmpty() && !meaning.isEmpty()) {
                        allWords.add(new Word(english, meaning, phonetic));
                        count++;
                    }
                } catch (Exception e) {
                    // 忽略错误行
                }
            }
            System.out.println("成功加载 JSONL 词库，共 " + count + " 个单词。");

        } catch (Exception e) {
            System.err.println("读取 JSONL 词库失败: " + e.getMessage());
            loadMockData();
        }
    }

    private void loadMockData() {
        allWords.add(new Word("abandon", "v. 放弃，抛弃", "US /əˈbændən/"));
    }

    public List<Word> getTodayPlan(int wordsPerDay) {
        todayNewWords.clear();
        List<Word> shuffled = new ArrayList<>(allWords);
        Collections.shuffle(shuffled);
        int actualSize = Math.min(wordsPerDay, shuffled.size());
        todayNewWords.addAll(shuffled.subList(0, actualSize));
        return todayNewWords;
    }

    public void addWeakWord(Word word) {
        for (Word w : weakWords) {
            if (w.english.equals(word.english)) return;
        }
        weakWords.add(word);
    }

    public List<Word> getWeakWords() {
        return weakWords;
    }

    public List<Word> generateTestQueue() {
        List<Word> testQueue = new ArrayList<>(todayNewWords);
        for (Word w : weakWords) {
            boolean exists = false;
            for (Word t : testQueue) {
                if (t.english.equals(w.english)) { exists = true; break; }
            }
            if (!exists) testQueue.add(w);
        }
        Collections.shuffle(testQueue);
        return testQueue;
    }
}