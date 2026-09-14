package com.zyy.papercheck;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link TextNormalizer} 的单元测试：验证规范化与特征提取的正确性。
 */
class TextNormalizerTest {

    @Test
    @DisplayName("全角字符与全角空格会被转成半角")
    void fullWidthCharactersAreConverted() {
        assertEquals("abc 123", TextNormalizer.normalize("ａｂｃ　１２３"));
    }

    @Test
    @DisplayName("英文大写会被统一成小写")
    void upperCaseIsLowered() {
        assertEquals("hello world", TextNormalizer.normalize("Hello World"));
    }

    @Test
    @DisplayName("BOM 与不可见控制字符会被丢弃")
    void bomAndControlCharactersAreDropped() {
        assertEquals("论文查重", TextNormalizer.normalize("\uFEFF论文\u0007查重"));
    }

    @Test
    @DisplayName("不同平台的换行符会被统一")
    void newLinesAreUnified() {
        assertEquals("a\nb\nc", TextNormalizer.normalize("a\r\nb\rc"));
    }

    @Test
    @DisplayName("特征提取只保留汉字、字母与数字")
    void featureCodePointsKeepLettersAndDigitsOnly() {
        int[] codes = TextNormalizer.toFeatureCodePoints(TextNormalizer.normalize("今天，天气晴！2024 年 OK"));
        assertArrayEquals(TextNormalizer.toFeatureCodePoints("今天天气晴2024年ok"), codes);
    }

    @Test
    @DisplayName("空输入返回空结果而不是抛异常")
    void emptyInputIsHandled() {
        assertEquals("", TextNormalizer.normalize(null));
        assertEquals("", TextNormalizer.normalize(""));
        assertEquals(0, TextNormalizer.toFeatureCodePoints(null).length);
    }
}
