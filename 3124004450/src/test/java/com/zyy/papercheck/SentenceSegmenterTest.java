package com.zyy.papercheck;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link SentenceSegmenter} 的单元测试：验证两级切分策略。
 */
class SentenceSegmenterTest {

    @Test
    @DisplayName("按中文句末标点切分")
    void splitsByChineseSentenceEndings() {
        List<int[]> sentences = SentenceSegmenter.segment("今天是星期天。天气晴！晚上看电影？");
        assertEquals(3, sentences.size());
        assertEquals(6, sentences.get(0).length);
    }

    @Test
    @DisplayName("过长的句子会按逗号等停顿标点细切")
    void splitsOverlongSentenceBySoftDelimiters() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 20; i++) {
            builder.append("这是第").append(i).append("个分句，");
        }
        List<int[]> sentences = SentenceSegmenter.segment(builder.toString());
        assertTrue(sentences.size() > 1, "长句应被细切成多句");
        for (int[] sentence : sentences) {
            assertTrue(sentence.length <= SentenceSegmenter.MAX_SENTENCE_LENGTH);
        }
    }

    @Test
    @DisplayName("完全没有标点的长文本会按固定窗口兜底切分")
    void textWithoutPunctuationIsChunked() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 500; i++) {
            builder.append('中');
        }
        List<int[]> sentences = SentenceSegmenter.segment(builder.toString());
        assertEquals(5, sentences.size());
        assertEquals(SentenceSegmenter.MAX_SENTENCE_LENGTH, sentences.get(0).length);
    }

    @Test
    @DisplayName("空白与纯标点输入不会产生句子")
    void blankInputProducesNoSentence() {
        assertTrue(SentenceSegmenter.segment("   \n\t ").isEmpty());
        assertTrue(SentenceSegmenter.segment("，。！？;;").isEmpty());
        assertTrue(SentenceSegmenter.segment("").isEmpty());
    }
}
