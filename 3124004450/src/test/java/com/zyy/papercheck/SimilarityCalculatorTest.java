package com.zyy.papercheck;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link SimilarityCalculator} 的单元测试。
 *
 * <p>用例设计思路（白盒 + 边界 + 性能）：</p>
 * <ol>
 *   <li>等价类：完全相同、完全不同、部分相同；</li>
 *   <li>边界值：空文件、纯标点、单字、超长文件；</li>
 *   <li>需求给出的样例；</li>
 *   <li>性能：大文件必须在 5 秒内给出答案。</li>
 * </ol>
 */
class SimilarityCalculatorTest {

    private final SimilarityCalculator calculator = new SimilarityCalculator();

    @Test
    @DisplayName("两篇文章完全相同时重复率为 1.00")
    void identicalDocumentsScoreOne() {
        String text = "今天是星期天，天气晴，今天晚上我要去看电影。";
        assertEquals(1.0, calculator.calculate(text, text), 1e-9);
    }

    @Test
    @DisplayName("两篇文章没有公共内容时重复率为 0.00")
    void unrelatedDocumentsScoreZero() {
        assertEquals(0.0, calculator.calculate("苹果香蕉西瓜", "量子力学导论讲义"), 1e-9);
    }

    @Test
    @DisplayName("作业样例：改写后的抄袭版应得到较高重复率")
    void assignmentSampleScoresHigh() {
        String original = "今天是星期天，天气晴，今天晚上我要去看电影。";
        String copied = "今天是周天，天气晴朗，我晚上要去看电影。";
        double rate = calculator.calculate(original, copied);
        // 18 个特征字符中有 14 个可以在原文中找到顺序一致的对应
        assertEquals(0.82, rate, 0.03);
    }

    @Test
    @DisplayName("抄袭版在原文基础上增加内容时，重复率仍然很高")
    void copyWithInsertionsKeepsHighScore() {
        String original = "深度学习模型的训练需要大量的数据和算力支持。";
        String copied = "深度学习模型的训练需要大量的数据和算力支持，此外还需要调参经验。";
        double rate = calculator.calculate(original, copied);
        assertTrue(rate > 0.6 && rate < 1.0, "实际重复率: " + rate);
    }

    @Test
    @DisplayName("抄袭版删掉一部分内容后，重复率接近全文")
    void copyWithDeletionsKeepsHighScore() {
        String original = "深度学习模型的训练需要大量的数据和算力支持。";
        String copied = "深度学习模型的训练需要大量数据和算力。";
        assertTrue(calculator.calculate(original, copied) > 0.8);
    }

    @Test
    @DisplayName("抄袭版替换少量字词后，重复率下降但仍然是正数")
    void copyWithReplacementLowersScore() {
        String original = "深度学习模型的训练需要大量的数据和算力支持。";
        String copied = "登山运动需要良好的体能和装备支持。";
        double rate = calculator.calculate(original, copied);
        assertTrue(rate > 0.0 && rate < 0.8, "实际重复率: " + rate);
    }

    @Test
    @DisplayName("原文为空时重复率为 0.00")
    void emptyOriginalScoresZero() {
        assertEquals(0.0, calculator.calculate("", "任意内容"), 1e-9);
    }

    @Test
    @DisplayName("抄袭版为空时重复率为 0.00")
    void emptyCopiedScoresZero() {
        assertEquals(0.0, calculator.calculate("任意内容", ""), 1e-9);
    }

    @Test
    @DisplayName("纯标点或纯空白输入不会导致异常，重复率为 0.00")
    void punctuationOnlyScoresZero() {
        assertEquals(0.0, calculator.calculate("，。！？;;", "……——！！"), 1e-9);
        assertEquals(0.0, calculator.calculate("    \n\t", "    \n\t"), 1e-9);
    }

    @Test
    @DisplayName("全角半角与大小写差异不影响结果")
    void caseAndWidthDifferencesAreIgnored() {
        assertEquals(1.0, calculator.calculate("Hello 世界 2024", "ＨＥＬＬＯ　世界　２０２４"), 1e-9);
    }

    @Test
    @DisplayName("单字句子也能被正确匹配")
    void singleCharacterSentenceIsMatched() {
        double rate = calculator.calculate("天。地。人。", "天。地。人。");
        assertEquals(1.0, rate, 1e-9);
    }

    @Test
    @DisplayName("抄袭句跨越原文句边界时仍然能被匹配")
    void sentenceSpanningOriginalBoundaryIsMatched() {
        // 抄袭版把原文第一句的后半段和第二句的前半段拼成了一句，
        // 单句比对只能匹配到一部分，必须把相邻原文句合并起来才能完全匹配
        String original = "今天天气很好，我们一起去公园散步吧。明天要下雨，记得带伞出门。";
        String copied = "一起去公园散步吧明天要下雨";
        double rate = calculator.calculate(original, copied);
        assertTrue(rate > 0.9, "实际重复率: " + rate);
    }

    @Test
    @DisplayName("重复率始终落在 [0, 1] 区间内")
    void resultIsAlwaysWithinUnitInterval() {
        String[][] pairs = {
                {"今天天气很好", "今天天气很好"},
                {"今天天气很好", "今天天气很糟"},
                {"abcdefg", "gfedcba"},
                {"这是一篇很长的论文正文内容。", "这是另一篇毫不相关的文章内容。"},
        };
        for (String[] pair : pairs) {
            double rate = calculator.calculate(pair[0], pair[1]);
            assertTrue(rate >= 0.0 && rate <= 1.0, "越界结果: " + rate);
        }
    }

    @Test
    @DisplayName("性能：约 13 万字的文档必须在 5 秒内给出答案")
    void largeDocumentIsProcessedWithinTimeBudget() {
        StringBuilder original = new StringBuilder();
        StringBuilder copied = new StringBuilder();
        for (int i = 0; i < 3000; i++) {
            String sentence = "第" + i + "段文字用于性能测试，本段内容考察查重算法在大规模输入下的时间开销与内存占用。";
            original.append(sentence);
            copied.append(i % 10 == 0 ? "第" + i + "段文字被完全改写了，本段与其他段落不再一致。" : sentence);
        }
        long start = System.nanoTime();
        double rate = calculator.calculate(original.toString(), copied.toString());
        long elapsedMillis = (System.nanoTime() - start) / 1000000L;
        assertTrue(rate > 0.5, "大文件重复率异常: " + rate);
        assertTrue(elapsedMillis < 5000L, "耗时 " + elapsedMillis + " ms，超过 5 秒限制");
    }
}
