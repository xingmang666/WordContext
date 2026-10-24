package org.example;

public class Word {
    public String english;
    public String meaning;
    public String phonetic; // 新增：音标字段

    public Word(String english, String meaning, String phonetic) {
        this.english = english;
        this.meaning = meaning;
        this.phonetic = phonetic;
    }

    // 兼容旧代码的构造方法
    public Word(String english, String meaning) {
        this(english, meaning, "");
    }
}