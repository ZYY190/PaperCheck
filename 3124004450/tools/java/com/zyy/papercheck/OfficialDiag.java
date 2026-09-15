package com.zyy.papercheck;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;

/**
 * 诊断程序：在教师下发的样例数据上对比「本实现」与「句级暴力精确解」，
 * 用于确认候选剪枝没有漏掉匹配。
 */
public final class OfficialDiag {

    public static void main(String[] args) throws Exception {
        Charset utf8 = Charset.forName("UTF-8");
        String originalText = new String(Files.readAllBytes(Paths.get(args[0])), utf8);
        String copiedText = new String(Files.readAllBytes(Paths.get(args[1])), utf8);

        List<int[]> original = SentenceSegmenter.segment(TextNormalizer.normalize(originalText));
        List<int[]> copied = SentenceSegmenter.segment(TextNormalizer.normalize(copiedText));

        double actual = new SimilarityCalculator().calculate(originalText, copiedText);

        LcsMatcher matcher = new LcsMatcher(256);
        int total = 0;
        int matched = 0;
        for (int[] sentence : copied) {
            total += sentence.length;
            int best = 0;
            for (int[] candidate : original) {
                int value = matcher.length(sentence, candidate);
                if (value > best) {
                    best = value;
                }
            }
            matched += best;
        }
        double brute = total == 0 ? 0.0 : (double) matched / total;

        // 全文级 LCS：作为「定义上的理想值」参考
        int[] left = TextNormalizer.toFeatureCodePoints(TextNormalizer.normalize(originalText));
        int[] right = TextNormalizer.toFeatureCodePoints(TextNormalizer.normalize(copiedText));
        int whole = new LcsMatcher(Math.max(left.length, right.length)).length(left, right);
        double wholeRate = right.length == 0 ? 0.0 : (double) whole / right.length;

        System.out.println(String.format(Locale.ROOT,
                "%s  本实现=%.4f  句级暴力精确解=%.4f  全文LCS=%.4f  (原文句=%d 抄袭句=%d)",
                Paths.get(args[1]).getFileName(), Double.valueOf(actual), Double.valueOf(brute),
                Double.valueOf(wholeRate), Integer.valueOf(original.size()), Integer.valueOf(copied.size())));
    }
}
