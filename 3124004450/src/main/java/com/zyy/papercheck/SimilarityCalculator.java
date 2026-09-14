package com.zyy.papercheck;

/**
 * 计算模块：重复率计算器（基础版本）。
 *
 * <p>把「重复率」定义为「抄袭版中有多少比例的内容能在原文里找到顺序一致的对应」，
 * 直接用两篇文章的全部有效字符做一次最长公共子序列（LCS）比对：</p>
 *
 * <pre>
 * 重复率 = LCS(原文, 抄袭版) / 抄袭版特征字符数
 * </pre>
 *
 * <p>实现简单、结果直观，缺点是时间与空间复杂度都是 O(n*m)，
 * 只适合几十 KB 以内的小文件。</p>
 *
 * @author zyy
 */
public final class SimilarityCalculator {

    /**
     * 计算两篇文章的重复率。
     *
     * @param originalText 原文内容
     * @param copiedText   抄袭版论文内容
     * @return 取值范围 [0, 1] 的重复率；任意一方没有有效内容时返回 0
     */
    public double calculate(String originalText, String copiedText) {
        if (originalText == null || copiedText == null) {
            return 0.0;
        }
        int[] left = TextNormalizer.toFeatureCodePoints(TextNormalizer.normalize(originalText));
        int[] right = TextNormalizer.toFeatureCodePoints(TextNormalizer.normalize(copiedText));
        if (left.length == 0 || right.length == 0) {
            return 0.0;
        }
        int common = new LcsMatcher(Math.min(left.length, right.length)).length(left, right);
        double rate = (double) common / right.length;
        return rate > 1.0 ? 1.0 : rate;
    }
}
