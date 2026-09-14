package com.zyy.papercheck;

import java.util.Arrays;

/**
 * 最长公共子序列（Longest Common Subsequence）计算器。
 *
 * <p>LCS 的长度直接回答了「抄袭版的这一句里，有多少个字能在原文中按相同顺序找到」，
 * 是衡量段落级重复程度最直观的指标。</p>
 *
 * <p>本类做了两点工程优化：</p>
 * <ol>
 *   <li>使用两行滚动数组，把空间复杂度从 O(n*m) 降到 O(min(n,m))，
 *       同时避免了每次比较都重新分配数组带来的 GC 压力；</li>
 *   <li>实例可重复使用，配合调用方的上界剪枝，大幅减少无效计算。</li>
 * </ol>
 *
 * @author zyy
 */
public final class LcsMatcher {

    /** 单次比较允许的最大规模（两个序列长度之积），超过时调用方应退化为近似算法。 */
    public static final long MAX_CELLS = 4000000L;

    /** 上一行结果；与 {@link #current} 在计算过程中交替使用。 */
    private int[] previous;

    /** 当前行结果；与 {@link #previous} 在计算过程中交替使用。 */
    private int[] current;

    /**
     * @param maxLength 允许比较的最长序列长度，决定滚动数组的大小
     */
    public LcsMatcher(int maxLength) {
        int capacity = Math.max(maxLength, 1);
        this.previous = new int[capacity + 1];
        this.current = new int[capacity + 1];
    }

    /**
     * 计算两个序列的最长公共子序列长度。
     *
     * @param first  序列一
     * @param second 序列二
     * @return 最长公共子序列的长度
     */
    public int length(int[] first, int[] second) {
        if (first.length == 0 || second.length == 0) {
            return 0;
        }
        int[] shorter = first;
        int[] longer = second;
        if (shorter.length > longer.length) {
            shorter = second;
            longer = first;
        }
        ensureCapacity(longer.length);
        // 每次调用都重置，保证实例可以被重复使用
        Arrays.fill(previous, 0);
        Arrays.fill(current, 0);
        for (int i = 1; i <= shorter.length; i++) {
            int codePoint = shorter[i - 1];
            current[0] = 0;
            for (int j = 1; j <= longer.length; j++) {
                if (codePoint == longer[j - 1]) {
                    current[j] = previous[j - 1] + 1;
                } else {
                    current[j] = previous[j] >= current[j - 1] ? previous[j] : current[j - 1];
                }
            }
            // 交换两行：current 的每一格在下一轮都会被重新写入，无需清零
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[longer.length];
    }

    /**
     * 防御式扩容：即使调用方估算的长度偏小，也不会抛出数组越界异常。
     *
     * @param requiredLength 本次比较所需的最大长度
     */
    private void ensureCapacity(int requiredLength) {
        if (previous.length <= requiredLength) {
            previous = new int[requiredLength + 1];
            current = new int[requiredLength + 1];
        }
    }
}
