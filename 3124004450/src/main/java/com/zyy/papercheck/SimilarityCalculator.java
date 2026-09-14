package com.zyy.papercheck;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 计算模块核心：重复率计算器。
 *
 * <p><b>算法思想</b>：把「重复率」定义为「抄袭版中有多少比例的内容，可以在原文里找到顺序一致的对应」。</p>
 *
 * <p><b>计算步骤</b>：</p>
 * <ol>
 *   <li>规范化：消除全角/半角、大小写、不可见字符等无关差异；</li>
 *   <li>分句：把文章切成句子，使比对代价有上界；</li>
 *   <li>建立原文的二元字组倒排索引；</li>
 *   <li>对抄袭版的每一句，用倒排索引筛出候选句，再用 LCS 求出最大公共子序列长度；</li>
 *   <li>重复率 = 各句最大公共子序列长度之和 / 抄袭版特征字符总数。</li>
 * </ol>
 *
 * @author zyy
 */
public final class SimilarityCalculator {

    /** 每个句子最多精确比较的候选句数量。 */
    static final int MAX_CANDIDATES = 32;

    /** 句子长度相差超过该倍数时直接判定为不可能高度重复。 */
    static final int LENGTH_RATIO = 6;

    /** 出现句子数超过该值的字组过于常见，不参与候选筛选。 */
    static final int POSTINGS_DF_LIMIT = 500;

    /** 单个句子扫描倒排表的最大条目数。 */
    static final int MAX_POSTINGS_VISITS = 4000;

    /** 候选句排序：命中字组数多的优先。 */
    private static final Comparator<Map.Entry<Integer, Integer>> CANDIDATE_ORDER =
            new Comparator<Map.Entry<Integer, Integer>>() {
                @Override
                public int compare(Map.Entry<Integer, Integer> left, Map.Entry<Integer, Integer> right) {
                    int byHits = right.getValue().compareTo(left.getValue());
                    return byHits != 0 ? byHits : left.getKey().compareTo(right.getKey());
                }
            };

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
        if (originalText.equals(copiedText)) {
            boolean hasFeature = TextNormalizer.toFeatureCodePoints(
                    TextNormalizer.normalize(originalText)).length > 0;
            return hasFeature ? 1.0 : 0.0;
        }
        List<int[]> originalSentences = SentenceSegmenter.segment(TextNormalizer.normalize(originalText));
        List<int[]> copiedSentences = SentenceSegmenter.segment(TextNormalizer.normalize(copiedText));
        if (originalSentences.isEmpty() || copiedSentences.isEmpty()) {
            return 0.0;
        }
        int copiedLength = countCodePoints(copiedSentences);
        if (copiedLength == 0) {
            return 0.0;
        }
        int matched = matchSentences(originalSentences, copiedSentences);
        double rate = (double) matched / copiedLength;
        if (rate < 0.0) {
            return 0.0;
        }
        return rate > 1.0 ? 1.0 : rate;
    }

    private static int countCodePoints(List<int[]> sentences) {
        int total = 0;
        for (int i = 0; i < sentences.size(); i++) {
            total += sentences.get(i).length;
        }
        return total;
    }

    private int matchSentences(List<int[]> original, List<int[]> copied) {
        NGramIndex index = new NGramIndex();
        Set<Integer> originalSingleChars = new HashSet<Integer>();
        int maxOriginalLength = 1;
        for (int id = 0; id < original.size(); id++) {
            int[] sentence = original.get(id);
            index.add(id, sentence);
            if (sentence.length == 1) {
                originalSingleChars.add(Integer.valueOf(sentence[0]));
            } else if (sentence.length > maxOriginalLength) {
                maxOriginalLength = sentence.length;
            }
        }
        LcsMatcher matcher = new LcsMatcher(Math.max(maxOriginalLength, maxLength(copied)));
        Map<Integer, Integer> hitCounter = new HashMap<Integer, Integer>();
        int matched = 0;
        for (int i = 0; i < copied.size(); i++) {
            matched += bestMatch(copied.get(i), original, index, matcher, originalSingleChars, hitCounter);
        }
        return matched;
    }

    private int bestMatch(int[] sentence, List<int[]> original, NGramIndex index,
                          LcsMatcher matcher, Set<Integer> originalSingleChars,
                          Map<Integer, Integer> hitCounter) {
        int length = sentence.length;
        if (length <= 0) {
            return 0;
        }
        if (length == 1) {
            return originalSingleChars.contains(Integer.valueOf(sentence[0])) ? 1 : 0;
        }
        collectCandidates(sentence, index, hitCounter, true);
        if (hitCounter.isEmpty()) {
            collectCandidates(sentence, index, hitCounter, false);
        }
        if (hitCounter.isEmpty()) {
            return 0;
        }
        List<Map.Entry<Integer, Integer>> candidates =
                new ArrayList<Map.Entry<Integer, Integer>>(hitCounter.entrySet());
        Collections.sort(candidates, CANDIDATE_ORDER);

        int best = 0;
        int examined = 0;
        for (int i = 0; i < candidates.size() && examined < MAX_CANDIDATES; i++) {
            Map.Entry<Integer, Integer> entry = candidates.get(i);
            int[] candidate = original.get(entry.getKey().intValue());
            int minLength = Math.min(candidate.length, length);
            int upperBound = Math.min(minLength, entry.getValue().intValue() + 1);
            if (upperBound <= best) {
                break;
            }
            if (candidate.length > (long) length * LENGTH_RATIO
                    || length > (long) candidate.length * LENGTH_RATIO) {
                continue;
            }
            examined++;
            int value;
            if ((long) candidate.length * length > LcsMatcher.MAX_CELLS) {
                value = upperBound;
            } else {
                value = matcher.length(sentence, candidate);
            }
            if (value > best) {
                best = value;
                if (best == minLength) {
                    break;
                }
            }
        }
        return best;
    }

    private void collectCandidates(int[] sentence, NGramIndex index,
                                   Map<Integer, Integer> out, boolean respectDfLimit) {
        out.clear();
        int visits = 0;
        for (int i = 0; i + 1 < sentence.length; i++) {
            int[] postings = index.lookup(sentence[i], sentence[i + 1]);
            if (postings.length == 0) {
                continue;
            }
            if (respectDfLimit && postings.length > POSTINGS_DF_LIMIT) {
                continue;
            }
            for (int k = 0; k < postings.length; k++) {
                if (visits++ >= MAX_POSTINGS_VISITS) {
                    return;
                }
                Integer id = Integer.valueOf(postings[k]);
                Integer hits = out.get(id);
                out.put(id, hits == null ? Integer.valueOf(1) : Integer.valueOf(hits.intValue() + 1));
            }
        }
    }

    private static int maxLength(List<int[]> sentences) {
        int max = 1;
        for (int i = 0; i < sentences.size(); i++) {
            if (sentences.get(i).length > max) {
                max = sentences.get(i).length;
            }
        }
        return max;
    }
}
