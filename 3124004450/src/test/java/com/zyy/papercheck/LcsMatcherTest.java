package com.zyy.papercheck;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link LcsMatcher} 的单元测试：验证最长公共子序列计算是否正确、实例能否被复用。
 */
class LcsMatcherTest {

    private final LcsMatcher matcher = new LcsMatcher(64);

    @Test
    @DisplayName("完全相同的序列，LCS 长度等于序列长度")
    void identicalSequencesReturnFullLength() {
        int[] sequence = codes("abcdef");
        assertEquals(6, matcher.length(sequence, sequence));
    }

    @Test
    @DisplayName("教科书用例：abcbdab 与 bdcaba 的 LCS 长度为 4")
    void classicExampleReturnsExpectedLength() {
        assertEquals(4, matcher.length(codes("abcbdab"), codes("bdcaba")));
    }

    @Test
    @DisplayName("没有任何公共字符时返回 0")
    void noCommonElementReturnsZero() {
        assertEquals(0, matcher.length(codes("abc"), codes("xyz")));
    }

    @Test
    @DisplayName("存在空序列时返回 0")
    void emptySequenceReturnsZero() {
        assertEquals(0, matcher.length(codes(""), codes("abc")));
        assertEquals(0, matcher.length(codes("abc"), codes("")));
    }

    @Test
    @DisplayName("同一个实例可以连续用于不同规模的比较")
    void matcherCanBeReusedAcrossDifferentSizes() {
        assertEquals(3, matcher.length(codes("abcd"), codes("abxd")));
        assertEquals(8, matcher.length(codes("abcdefgh"), codes("abcdefgh")));
        assertEquals(2, matcher.length(codes("ab"), codes("ab")));
        assertEquals(0, matcher.length(codes("ab"), codes("cd")));
    }

    @Test
    @DisplayName("实际序列超出构造时给定的容量也能正常计算")
    void handlesSequencesLongerThanDeclaredCapacity() {
        LcsMatcher small = new LcsMatcher(2);
        assertEquals(20, small.length(codes("abcdefghijklmnopqrst"), codes("abcdefghijklmnopqrst")));
    }

    private static int[] codes(String text) {
        return TextNormalizer.toFeatureCodePoints(TextNormalizer.normalize(text));
    }
}
