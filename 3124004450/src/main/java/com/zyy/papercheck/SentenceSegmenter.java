package com.zyy.papercheck;

import java.util.ArrayList;
import java.util.List;

/**
 * 分句器。
 *
 * <p>直接对整篇文章做序列比对的时间与空间开销都是平方级，无法在 5 秒内处理大文件。
 * 因此先把文章切成句子，再以句子为单位匹配，把平方级开销限制在「句长 × 句长」这一可控范围内。</p>
 *
 * <p>切分采用两级策略：</p>
 * <ol>
 *   <li>一级：按句末标点（。！？；!?;…）与换行切分；</li>
 *   <li>二级：若某句特征字符数超过 {@link #MAX_SENTENCE_LENGTH}，
 *       再按句内停顿标点（，,、：:）细切；仍超长时按固定窗口兜底切分。</li>
 * </ol>
 *
 * @author zyy
 */
public final class SentenceSegmenter {

    /** 一级分隔符：句末标点与换行。 */
    static final String HARD_DELIMITERS = "。！？；!?;…\n";

    /** 二级分隔符：句内停顿标点。 */
    static final String SOFT_DELIMITERS = "，,、：:";

    /** 单句特征字符数上限，超过则继续细切，保证匹配代价有上界。 */
    static final int MAX_SENTENCE_LENGTH = 100;

    private SentenceSegmenter() {
    }

    /**
     * 把标准文本切分成句子，并转换成特征码点数组。
     *
     * @param normalizedText 已经过 {@link TextNormalizer#normalize(String)} 处理的文本
     * @return 句子列表，每个元素是该句的特征码点数组；不含空句
     */
    public static List<int[]> segment(String normalizedText) {
        List<int[]> sentences = new ArrayList<int[]>();
        if (normalizedText == null || normalizedText.isEmpty()) {
            return sentences;
        }
        for (String rawSentence : splitBy(normalizedText, HARD_DELIMITERS)) {
            appendAdaptive(rawSentence, sentences);
        }
        return sentences;
    }

    /**
     * 按分隔符集合切分文本，分隔符本身被丢弃。
     *
     * @param text       待切分文本
     * @param delimiters 分隔符集合
     * @return 切分结果，不含空串
     */
    static List<String> splitBy(String text, String delimiters) {
        List<String> parts = new ArrayList<String>();
        int start = 0;
        for (int i = 0; i < text.length(); i++) {
            if (delimiters.indexOf(text.charAt(i)) >= 0) {
                if (i > start) {
                    parts.add(text.substring(start, i));
                }
                start = i + 1;
            }
        }
        if (start < text.length()) {
            parts.add(text.substring(start));
        }
        return parts;
    }

    /**
     * 对一句话做自适应切分：短句直接保留，长句按软标点再切，仍超长则按固定窗口切。
     */
    private static void appendAdaptive(String rawSentence, List<int[]> out) {
        int[] codes = TextNormalizer.toFeatureCodePoints(rawSentence);
        if (codes.length == 0) {
            return;
        }
        if (codes.length <= MAX_SENTENCE_LENGTH) {
            out.add(codes);
            return;
        }
        for (String piece : splitBy(rawSentence, SOFT_DELIMITERS)) {
            int[] pieceCodes = TextNormalizer.toFeatureCodePoints(piece);
            if (pieceCodes.length == 0) {
                continue;
            }
            if (pieceCodes.length <= MAX_SENTENCE_LENGTH) {
                out.add(pieceCodes);
            } else {
                appendChunks(pieceCodes, out);
            }
        }
    }

    /**
     * 兜底切分：整段文字没有任何标点时，按固定窗口切片。
     */
    private static void appendChunks(int[] codes, List<int[]> out) {
        for (int start = 0; start < codes.length; start += MAX_SENTENCE_LENGTH) {
            int end = Math.min(start + MAX_SENTENCE_LENGTH, codes.length);
            int[] chunk = new int[end - start];
            System.arraycopy(codes, start, chunk, 0, chunk.length);
            out.add(chunk);
        }
    }
}
