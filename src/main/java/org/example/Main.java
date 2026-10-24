package org.example;

import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Main {
    private JFrame frame;
    private JPanel mainPanel;
    private CardLayout cardLayout;

    private WordManager wordManager = new WordManager();

    private List<Word> todayNewWords;
    private List<Word> testQueue;
    private int currentWordIndex = 0;
    private int currentTestIndex = 0;
    private boolean isMeaningShown = false;
    private boolean isTestAnswered = false;

    private JLabel progressLabel, wordLabel, phoneticLabel, meaningLabel, testMeaningLabel;
    private JTextField testInputField;
    private JLabel feedbackLabel, loadingLabel;
    private JButton testActionBtn;

    private JTextArea passageArea;
    private JPanel questionsPanel;
    private List<Question> parsedQuestions = new ArrayList<>();
    private List<ButtonGroup> buttonGroups = new ArrayList<>();
    private List<JTextField> grammarFields = new ArrayList<>();
    private String currentExamType = "";

    private List<Word> testWrongWords = new ArrayList<>();
    private List<Word> testCorrectWords = new ArrayList<>();

    private static final Color BG_COLOR = new Color(242, 244, 247);
    private static final Color CARD_COLOR = Color.WHITE;
    private static final Color PRIMARY_COLOR = new Color(41, 98, 255);
    private static final Color TEXT_DARK = new Color(33, 37, 41);
    private static final Color TEXT_LIGHT = new Color(108, 117, 125);

    public static void main(String[] args) {
        // 1. 无控制台环境下的护身符：将输出重定向到文件，防止 IO 异常导致闪退
        try {
            java.io.File logFile = new java.io.File("error.log");
            System.setOut(new java.io.PrintStream(new java.io.FileOutputStream(logFile)));
            System.setErr(new java.io.PrintStream(new java.io.FileOutputStream(logFile)));
        } catch (Exception ignored) {}

        // 2. 全局异常捕获：如果真报错了，把错误信息写进日志，不闪退
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.FileWriter("error.log", true))) {
                e.printStackTrace(pw);
            } catch (Exception ignored) {}
        });

        // 3. 正常启动 UI
        UIManager.put("Panel.background", BG_COLOR);
        UIManager.put("Component.arc", 20);
        UIManager.put("Button.arc", 20);
        UIManager.put("TextComponent.arc", 15);
        UIManager.put("Component.focusWidth", 0);
        UIManager.put("Button.background", CARD_COLOR);
        UIManager.put("Button.font", new Font("微软雅黑", Font.BOLD, 14));
        try { FlatLightLaf.setup(); } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new Main().initUI());
    }

    public void initUI() {
        // 修改：窗口标题加上 Beta 版本号
        frame = new JFrame("词境 - 智能单词本 (Early Beta v0.1)");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(550, 700);
        frame.setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        mainPanel.setBackground(BG_COLOR);

        mainPanel.add(createHomePanel(), "HOME");
        mainPanel.add(createLearnPanel(), "LEARN");
        mainPanel.add(createTestPanel(), "TEST");
        mainPanel.add(createCompletePanel(), "COMPLETE");
        mainPanel.add(createPracticePanel(), "PRACTICE");

        frame.add(mainPanel);
        frame.setVisible(true);
    }

    private JPanel createHomePanel() {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(BG_COLOR);
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(CARD_COLOR);
        card.setBorder(new EmptyBorder(40, 50, 40, 50));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.gridx = 0; gbc.gridy = 0;

        JLabel title = new JLabel("智能背单词系统");
        title.setFont(new Font("微软雅黑", Font.BOLD, 32));
        title.setForeground(TEXT_DARK);
        card.add(title, gbc);

        gbc.gridy++;
        JButton startBtn = new JButton("开始今日学习");
        startBtn.setFont(new Font("微软雅黑", Font.BOLD, 16));
        startBtn.setPreferredSize(new Dimension(250, 48));
        startBtn.setBackground(PRIMARY_COLOR);
        startBtn.setForeground(Color.WHITE);
        startBtn.putClientProperty("JButton.buttonType", "roundRect");
        startBtn.addActionListener(e -> startLearning());
        card.add(startBtn, gbc);

        gbc.gridy++;
        JButton settingsBtn = new JButton("⚙ 设置 API Key");
        settingsBtn.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        settingsBtn.setPreferredSize(new Dimension(250, 35));
        settingsBtn.putClientProperty("JButton.buttonType", "roundRect");
        settingsBtn.addActionListener(e -> {
            String currentKey = AIService.getApiKey();
            String newKey = JOptionPane.showInputDialog(frame,
                    "请输入你的 DeepSeek API Key：\n(留空则使用离线演示模式)",
                    currentKey == null ? "" : currentKey);
            if (newKey != null) {
                AIService.saveApiKey(newKey);
                JOptionPane.showMessageDialog(frame, "配置已保存！", "提示", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        card.add(settingsBtn, gbc);

        // --- 新增：Beta 版本免责声明 ---
        gbc.gridy++;
        gbc.insets = new Insets(30, 10, 0, 10);
        JLabel versionLabel = new JLabel("Early Beta v0.1 | 科创赛事演示版，AI内容仅供参考", SwingConstants.CENTER);
        versionLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        versionLabel.setForeground(new Color(150, 150, 150));
        card.add(versionLabel, gbc);
        // ------------------------------

        wrapper.add(card);
        return wrapper;
    }

    private JPanel createLearnPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_COLOR);
        progressLabel = new JLabel("今日新词: 1 / 10");
        progressLabel.setFont(new Font("微软雅黑", Font.BOLD, 16));
        progressLabel.setForeground(TEXT_LIGHT);
        progressLabel.setBorder(new EmptyBorder(30, 40, 20, 20));
        panel.add(progressLabel, BorderLayout.NORTH);

        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setBackground(BG_COLOR);
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(CARD_COLOR);
        card.setPreferredSize(new Dimension(450, 300));
        card.setBorder(new EmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0; gbc.insets = new Insets(10, 0, 5, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        wordLabel = new JLabel("", SwingConstants.CENTER);
        wordLabel.setFont(new Font("Arial", Font.BOLD, 52));
        wordLabel.setForeground(TEXT_DARK);
        card.add(wordLabel, gbc);

        gbc.gridy++;
        phoneticLabel = new JLabel("", SwingConstants.CENTER);
        phoneticLabel.setFont(new Font("Arial", Font.PLAIN, 18));
        phoneticLabel.setForeground(TEXT_LIGHT);
        card.add(phoneticLabel, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(15, 0, 20, 0);
        meaningLabel = new JLabel(" ", SwingConstants.CENTER);
        meaningLabel.setFont(new Font("微软雅黑", Font.PLAIN, 22));
        meaningLabel.setForeground(new Color(25, 135, 84));
        card.add(meaningLabel, gbc);

        centerWrapper.add(card);
        panel.add(centerWrapper, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 40));
        bottomPanel.setBackground(BG_COLOR);
        JButton showMeaningBtn = new JButton("显示释义");
        showMeaningBtn.setPreferredSize(new Dimension(130, 45));
        showMeaningBtn.putClientProperty("JButton.buttonType", "roundRect");
        JButton nextBtn = new JButton("下一个");
        nextBtn.setPreferredSize(new Dimension(130, 45));
        nextBtn.setBackground(PRIMARY_COLOR);
        nextBtn.setForeground(Color.WHITE);

        showMeaningBtn.addActionListener(e -> {
            meaningLabel.setText(todayNewWords.get(currentWordIndex).meaning);
            isMeaningShown = true;
        });

        nextBtn.addActionListener(e -> {
            if (!isMeaningShown) wordManager.addWeakWord(todayNewWords.get(currentWordIndex));
            currentWordIndex++;
            if (currentWordIndex < todayNewWords.size()) loadNextWord();
            else startTest();
        });
        bottomPanel.add(showMeaningBtn);
        bottomPanel.add(nextBtn);
        panel.add(bottomPanel, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createTestPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_COLOR);
        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setBackground(BG_COLOR);
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(CARD_COLOR);
        card.setPreferredSize(new Dimension(450, 300));
        card.setBorder(new EmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.insets = new Insets(5, 0, 15, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        JLabel hint = new JLabel("请根据中文写出英文", SwingConstants.CENTER);
        hint.setFont(new Font("微软雅黑", Font.PLAIN, 16));
        hint.setForeground(TEXT_LIGHT);
        card.add(hint, gbc);

        gbc.gridy++;
        testMeaningLabel = new JLabel("", SwingConstants.CENTER);
        testMeaningLabel.setFont(new Font("微软雅黑", Font.BOLD, 24));
        testMeaningLabel.setForeground(TEXT_DARK);
        card.add(testMeaningLabel, gbc);

        gbc.gridy++;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        gbc.insets = new Insets(10, 0, 10, 0);

        testInputField = new JTextField(15);
        testInputField.setFont(new Font("微软雅黑", Font.PLAIN, 22));
        testInputField.setHorizontalAlignment(JTextField.CENTER);
        testInputField.setPreferredSize(new Dimension(300, 50));
        testInputField.setMinimumSize(new Dimension(300, 50));
        card.add(testInputField, gbc);

        gbc.gridy++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        feedbackLabel = new JLabel(" ", SwingConstants.CENTER);
        feedbackLabel.setFont(new Font("微软雅黑", Font.BOLD, 16));
        card.add(feedbackLabel, gbc);

        centerWrapper.add(card);
        panel.add(centerWrapper, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 40));
        bottomPanel.setBackground(BG_COLOR);
        testActionBtn = new JButton("提交答案");
        testActionBtn.setFont(new Font("微软雅黑", Font.BOLD, 16));
        testActionBtn.setPreferredSize(new Dimension(250, 48));
        testActionBtn.setBackground(PRIMARY_COLOR);
        testActionBtn.setForeground(Color.WHITE);

        testActionBtn.addActionListener(e -> {
            if (!isTestAnswered) {
                submitTestAnswer();
            } else {
                nextTestQuestion();
            }
        });

        bottomPanel.add(testActionBtn);
        panel.add(bottomPanel, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createCompletePanel() {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(BG_COLOR);
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(CARD_COLOR);
        card.setBorder(new EmptyBorder(40, 50, 40, 50));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("🎉 今日任务完成！", SwingConstants.CENTER);
        title.setFont(new Font("微软雅黑", Font.BOLD, 28));
        title.setForeground(TEXT_DARK);
        card.add(title, gbc);

        gbc.gridy++;
        JLabel subTitle = new JLabel("选择生成专属练习：", SwingConstants.CENTER);
        subTitle.setFont(new Font("微软雅黑", Font.PLAIN, 16));
        subTitle.setForeground(TEXT_LIGHT);
        card.add(subTitle, gbc);

        gbc.gridy++;
        String[] types = {"完形填空", "语法填空"};
        JComboBox<String> typeCombo = new JComboBox<>(types);
        typeCombo.setPreferredSize(new Dimension(200, 40));

        JPanel comboWrapper = new JPanel();
        comboWrapper.setBackground(CARD_COLOR);
        comboWrapper.add(typeCombo);
        card.add(comboWrapper, gbc);

        gbc.gridy++;
        JButton aiBtn = new JButton("✨ 生成专属练习");
        aiBtn.setFont(new Font("微软雅黑", Font.BOLD, 16));
        aiBtn.setPreferredSize(new Dimension(250, 50));
        aiBtn.setBackground(new Color(255, 153, 0));
        aiBtn.setForeground(Color.WHITE);

        JPanel btnWrapper = new JPanel();
        btnWrapper.setBackground(CARD_COLOR);
        btnWrapper.add(aiBtn);
        card.add(btnWrapper, gbc);

        gbc.gridy++;
        loadingLabel = new JLabel(" ", SwingConstants.CENTER);
        loadingLabel.setForeground(PRIMARY_COLOR);
        loadingLabel.setPreferredSize(new Dimension(300, 30));
        card.add(loadingLabel, gbc);

        aiBtn.addActionListener(e -> {
            currentExamType = (String) typeCombo.getSelectedItem();
            aiBtn.setEnabled(false);
            loadingLabel.setText("AI 正在生成" + currentExamType + "，请稍候...");

            new Thread(() -> {
                String result = AIService.generateExercise(todayNewWords, currentExamType);
                SwingUtilities.invokeLater(() -> {
                    aiBtn.setEnabled(true);
                    loadingLabel.setText(" ");
                    parseAndStartPractice(result);
                });
            }).start();
        });

        wrapper.add(card);
        return wrapper;
    }

    private JPanel createPracticePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_COLOR);

        passageArea = new JTextArea();
        passageArea.setFont(new Font("微软雅黑", Font.PLAIN, 16));
        passageArea.setLineWrap(true);
        passageArea.setWrapStyleWord(true);
        passageArea.setEditable(false);
        passageArea.setBorder(new EmptyBorder(20, 30, 20, 30));
        JScrollPane scrollPane = new JScrollPane(passageArea);
        scrollPane.setBorder(null);
        scrollPane.setPreferredSize(new Dimension(550, 200));

        questionsPanel = new JPanel();
        questionsPanel.setLayout(new BoxLayout(questionsPanel, BoxLayout.Y_AXIS));
        questionsPanel.setBackground(CARD_COLOR);
        questionsPanel.setBorder(new EmptyBorder(10, 30, 10, 30));

        JScrollPane questionScroll = new JScrollPane(questionsPanel);
        questionScroll.setBorder(null);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(scrollPane, BorderLayout.NORTH);
        centerPanel.add(questionScroll, BorderLayout.CENTER);
        panel.add(centerPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 20));
        bottomPanel.setBackground(BG_COLOR);
        JButton submitExamBtn = new JButton("提交试卷");
        submitExamBtn.setPreferredSize(new Dimension(250, 48));
        submitExamBtn.setBackground(PRIMARY_COLOR);
        submitExamBtn.setForeground(Color.WHITE);
        submitExamBtn.addActionListener(e -> checkExamAnswers());
        bottomPanel.add(submitExamBtn);
        panel.add(bottomPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void startLearning() {
        todayNewWords = wordManager.getTodayPlan(10);
        currentWordIndex = 0;
        loadNextWord();
        cardLayout.show(mainPanel, "LEARN");
    }

    private void loadNextWord() {
        isMeaningShown = false;
        Word currentWord = todayNewWords.get(currentWordIndex);
        progressLabel.setText("今日新词: " + (currentWordIndex + 1) + " / " + todayNewWords.size());
        wordLabel.setText(currentWord.english);
        phoneticLabel.setText(currentWord.phonetic);
        meaningLabel.setText(" ");
    }

    private void startTest() {
        testQueue = wordManager.generateTestQueue();
        currentTestIndex = 0;
        testWrongWords.clear();
        testCorrectWords.clear();
        loadNextTestQuestion();
        cardLayout.show(mainPanel, "TEST");
    }

    private void loadNextTestQuestion() {
        if (currentTestIndex < testQueue.size()) {
            isTestAnswered = false;
            testActionBtn.setText("提交答案");
            testInputField.setEnabled(true);
            testInputField.setText("");
            feedbackLabel.setText(" ");
            testMeaningLabel.setText(testQueue.get(currentTestIndex).meaning);
            testInputField.requestFocusInWindow();
        } else {
            showTestResults();
        }
    }

    private void submitTestAnswer() {
        String input = testInputField.getText().trim();
        if (input.isEmpty()) return;

        Word currentWord = testQueue.get(currentTestIndex);
        isTestAnswered = true;
        testInputField.setEnabled(false);
        testActionBtn.setText("下一题");

        if (input.equalsIgnoreCase(currentWord.english)) {
            feedbackLabel.setForeground(new Color(25, 135, 84));
            feedbackLabel.setText("✅ 回答正确！");
            if (!testCorrectWords.contains(currentWord)) testCorrectWords.add(currentWord);
        } else {
            feedbackLabel.setForeground(new Color(220, 53, 69));
            feedbackLabel.setText("❌ 错误！正确答案是: " + currentWord.english);
            wordManager.addWeakWord(currentWord);
            if (!testWrongWords.contains(currentWord)) testWrongWords.add(currentWord);
        }
    }

    private void nextTestQuestion() {
        currentTestIndex++;
        loadNextTestQuestion();
    }

    private void showTestResults() {
        StringBuilder resultStr = new StringBuilder("【默写成绩单】\n\n");
        resultStr.append("✅ 正确: ").append(testCorrectWords.size()).append(" 个\n");
        resultStr.append("❌ 错误: ").append(testWrongWords.size()).append(" 个\n\n");

        if (!testWrongWords.isEmpty()) {
            resultStr.append("以下单词已加入生词本，请后续重点复习：\n");
            for (Word w : testWrongWords) {
                resultStr.append("- ").append(w.english).append(" (").append(w.meaning.split("\n")[0]).append(")\n");
            }
        }

        JTextArea resultArea = new JTextArea(resultStr.toString());
        resultArea.setFont(new Font("微软雅黑", Font.PLAIN, 15));
        resultArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(resultArea);
        scrollPane.setPreferredSize(new Dimension(400, 300));

        JOptionPane.showMessageDialog(frame, scrollPane, "测试完成", JOptionPane.INFORMATION_MESSAGE);

        cardLayout.show(mainPanel, "COMPLETE");
    }

    private void parseAndStartPractice(String result) {
        parsedQuestions.clear();
        buttonGroups.clear();
        grammarFields.clear();
        questionsPanel.removeAll();
        passageArea.setText("");

        try {
            String passage = "";
            String[] parts = result.split("QUESTIONS:");
            if (parts.length > 0) {
                passage = parts[0].replace("PASSAGE:", "").trim();
            }
            passageArea.setText(passage);

            if (parts.length > 1) {
                String questionStr = parts[1].trim();
                String[] lines = questionStr.split("\n");
                for (String line : lines) {
                    if (line.trim().isEmpty()) continue;
                    String[] qParts = line.split("\\|");

                    Question q = new Question();

                    if ("完形填空".equals(currentExamType)) {
                        if (qParts.length >= 7) {
                            q.number = Integer.parseInt(qParts[0].trim());
                            q.options = new ArrayList<>();
                            for (int i = 1; i <= 4; i++) q.options.add(qParts[i].trim());
                            q.answer = qParts[5].trim();
                            q.explanation = qParts[6].trim();
                            parsedQuestions.add(q);
                        }
                    } else {
                        if (qParts.length >= 3) {
                            q.number = Integer.parseInt(qParts[0].trim());
                            q.options = new ArrayList<>();
                            q.answer = qParts[1].trim();
                            q.explanation = qParts[2].trim();
                            parsedQuestions.add(q);
                        }
                    }
                }
            }

            for (Question q : parsedQuestions) {
                JPanel qPanel = new JPanel(new GridLayout(0, 1, 5, 5));
                qPanel.setBackground(CARD_COLOR);
                qPanel.setBorder(new EmptyBorder(10, 0, 10, 0));

                JLabel qLabel = new JLabel("第 " + q.number + " 题：");
                qLabel.setFont(new Font("微软雅黑", Font.BOLD, 16));
                qPanel.add(qLabel);

                if ("完形填空".equals(currentExamType)) {
                    ButtonGroup bg = new ButtonGroup();
                    buttonGroups.add(bg);
                    grammarFields.add(null);
                    for (String opt : q.options) {
                        JRadioButton radio = new JRadioButton(opt);
                        radio.setBackground(CARD_COLOR);
                        radio.setFont(new Font("微软雅黑", Font.PLAIN, 15));
                        bg.add(radio);
                        qPanel.add(radio);
                    }
                } else {
                    JTextField inputField = new JTextField();
                    inputField.setFont(new Font("微软雅黑", Font.PLAIN, 18));
                    inputField.setPreferredSize(new Dimension(200, 35));
                    JPanel inputWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 5));
                    inputWrapper.setBackground(CARD_COLOR);
                    inputWrapper.add(inputField);
                    qPanel.add(inputWrapper);

                    buttonGroups.add(null);
                    grammarFields.add(inputField);
                }
                questionsPanel.add(qPanel);
            }
            questionsPanel.revalidate();
            questionsPanel.repaint();

            cardLayout.show(mainPanel, "PRACTICE");

        } catch (Exception ex) {
            passageArea.setText("解析AI生成内容失败。原始内容如下：\n" + result);
            cardLayout.show(mainPanel, "PRACTICE");
        }
    }

    private void checkExamAnswers() {
        int correctCount = 0;
        StringBuilder resultStr = new StringBuilder("【成绩单】\n\n");

        for (int i = 0; i < parsedQuestions.size(); i++) {
            Question q = parsedQuestions.get(i);
            String selected = "";

            if ("完形填空".equals(currentExamType)) {
                ButtonGroup bg = buttonGroups.get(i);
                if (bg != null) {
                    for (java.util.Enumeration<AbstractButton> buttons = bg.getElements(); buttons.hasMoreElements();) {
                        AbstractButton button = buttons.nextElement();
                        if (button.isSelected()) {
                            selected = button.getText().substring(0, 1);
                            break;
                        }
                    }
                }
            } else {
                JTextField field = grammarFields.get(i);
                if (field != null) {
                    selected = field.getText().trim();
                }
            }

            if (selected.equalsIgnoreCase(q.answer)) {
                correctCount++;
                resultStr.append("第 ").append(q.number).append(" 题: ✅ 正确\n");
            } else {
                resultStr.append("第 ").append(q.number).append(" 题: ❌ 错误\n");
                resultStr.append("你的答案: ").append(selected.isEmpty() ? "未作答" : selected).append("\n");
                resultStr.append("正确答案: ").append(q.answer).append("\n");
            }
            resultStr.append("解析: ").append(q.explanation).append("\n\n");
        }

        resultStr.insert(0, "总得分: " + correctCount + " / " + parsedQuestions.size() + "\n\n");

        JTextArea resultArea = new JTextArea(resultStr.toString());
        resultArea.setFont(new Font("微软雅黑", Font.PLAIN, 15));
        resultArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(resultArea);
        scrollPane.setPreferredSize(new Dimension(450, 400));

        JOptionPane.showMessageDialog(frame, scrollPane, "考试结果与分析", JOptionPane.INFORMATION_MESSAGE);
        cardLayout.show(mainPanel, "HOME");
    }
}