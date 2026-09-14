package com.zyy.papercheck;

import java.util.HashMap;
import java.util.Map;

/**
 * 二元字组（bigram）倒排索引。
 *
 * <p>索引把「连续的任意两个字」映射到「出现过它的句子编号」。
 * 匹配阶段先用它快速筛出可能重复的候选句，再对候选句做精确的序列比对，
 * 从而避免任意两句都两两比较。</p>
 *
 * @author zyy
 */
public final class NGramIndex {

    /** 字组的长度，取 2（二元组）在中文文本上兼顾区分度与索引体积。 */
    static final int GRAM_SIZE = 2;

    /** 二元组 -> 句子编号列表。 */
    private final Map<Long, IntList> postings = new HashMap<Long, IntList>();

    /**
     * 把一个句子登记到索引中。
     *
     * @param sentenceId 句子编号
     * @param codePoints 该句的特征码点
     */
    public void add(int sentenceId, int[] codePoints) {
        for (int i = 0; i + GRAM_SIZE <= codePoints.length; i++) {
            Long key = Long.valueOf(key(codePoints[i], codePoints[i + 1]));
            IntList list = postings.get(key);
            if (list == null) {
                list = new IntList(2);
                postings.put(key, list);
            }
            // 同一个句子内重复出现的字组只登记一次，保证 df 的含义是「句子数」
            if (list.size() == 0 || list.last() != sentenceId) {
                list.add(sentenceId);
            }
        }
    }

    /**
     * 查询某个字组出现过的所有句子编号。
     *
     * @param first  前一个字
     * @param second 后一个字
     * @return 句子编号数组；没有命中时返回长度为 0 的数组，不会返回 {@code null}
     */
    public int[] lookup(int first, int second) {
        IntList list = postings.get(Long.valueOf(key(first, second)));
        return list == null ? EMPTY : list.toArray();
    }

    /**
     * @return 索引中不同字组的个数，用于性能统计与单元测试
     */
    public int gramCount() {
        return postings.size();
    }

    /**
     * 把两个字合成一个 64 位键。汉字码点不超过 21 位，因此不会产生键冲突。
     */
    private static long key(int first, int second) {
        return ((long) first << 21) | (second & 0x1FFFFFL);
    }

    /** 空结果常量，避免每次查询都新建数组。 */
    private static final int[] EMPTY = new int[0];

    /**
     * 极简的可增长 int 列表，替代 {@code List<Integer>} 以减少装箱与内存开销。
     */
    private static final class IntList {

        private int[] data;
        private int size;

        IntList(int initialCapacity) {
            this.data = new int[Math.max(initialCapacity, 2)];
        }

        void add(int value) {
            if (size == data.length) {
                int[] grown = new int[data.length * 2];
                System.arraycopy(data, 0, grown, 0, size);
                data = grown;
            }
            data[size++] = value;
        }

        int size() {
            return size;
        }

        int last() {
            return data[size - 1];
        }

        int[] toArray() {
            int[] copy = new int[size];
            System.arraycopy(data, 0, copy, 0, size);
            return copy;
        }
    }
}
