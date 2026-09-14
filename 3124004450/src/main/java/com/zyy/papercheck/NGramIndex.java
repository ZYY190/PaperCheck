package com.zyy.papercheck;

/**
 * 二元字组（bigram）倒排索引。
 *
 * <p>索引把「连续的任意两个字」映射到「出现过它的句子编号」。
 * 匹配阶段先用它快速筛出可能重复的候选句，再对候选句做精确的序列比对，
 * 从而避免任意两句都两两比较。</p>
 *
 * <p>索引一旦建立便不再修改，因此不是线程安全的，但可以按只读方式被多次查询。</p>
 *
 * <p><b>性能设计</b>：键是两个字组合而成的 64 位整数，若直接使用
 * {@code HashMap<Long, ?>} 会在每次查询时产生装箱对象。这里改用开放地址法的
 * 原始类型哈希表，查询过程零对象分配，在大文件下可以减少大量 GC 压力。</p>
 *
 * @author zyy
 */
public final class NGramIndex {

    /** 字组的长度，取 2（二元组）在中文文本上兼顾区分度与索引体积。 */
    static final int GRAM_SIZE = 2;

    /** 初始桶数量，取 2 的幂便于用位运算取模。 */
    private static final int INITIAL_CAPACITY = 1 << 12;

    /** 键数组；0 表示空槽位（码点恒大于 0，因此键不会为 0）。 */
    private long[] keys = new long[INITIAL_CAPACITY];

    /** 与 {@link #keys} 一一对应的句子编号列表。 */
    private IntList[] values = new IntList[INITIAL_CAPACITY];

    /** 掩码，等于「桶数量 - 1」。 */
    private int mask = INITIAL_CAPACITY - 1;

    /** 已使用的桶数量。 */
    private int used;

    /**
     * 把一个句子登记到索引中。
     *
     * @param sentenceId 句子编号
     * @param codePoints 该句的特征码点
     */
    public void add(int sentenceId, int[] codePoints) {
        for (int i = 0; i + GRAM_SIZE <= codePoints.length; i++) {
            long key = key(codePoints[i], codePoints[i + 1]);
            IntList list = findByKey(key);
            if (list == null) {
                if ((used + 1) * 2 > keys.length) {
                    grow();
                }
                list = new IntList(2);
                insert(key, list);
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
        IntList list = findByKey(key(first, second));
        return list == null ? EMPTY : list.toArray();
    }

    /**
     * @return 索引中不同字组的个数，用于性能统计与单元测试
     */
    public int gramCount() {
        return used;
    }

    /**
     * 把两个字合成一个 64 位键。汉字码点不超过 21 位，因此不会产生键冲突。
     */
    private static long key(int first, int second) {
        return ((long) first << 21) | (second & 0x1FFFFFL);
    }

    /**
     * 在哈希表中查找键对应的列表。热路径专用的包内方法，避免返回数组副本。
     *
     * @param first  前一个字
     * @param second 后一个字
     * @return 句子编号列表；未命中返回 {@code null}
     */
    IntList postingsOf(int first, int second) {
        return findByKey(key(first, second));
    }

    /**
     * 开放地址法查找。
     *
     * @param key 二元组键
     * @return 对应的列表，未命中返回 {@code null}
     */
    private IntList findByKey(long key) {
        int slot = spread(key) & mask;
        while (true) {
            long current = keys[slot];
            if (current == 0L) {
                return null;
            }
            if (current == key) {
                return values[slot];
            }
            slot = (slot + 1) & mask;
        }
    }

    /**
     * 把键值对写入哈希表（调用方需先保证存在空槽）。
     *
     * @param key   二元组键
     * @param value 句子编号列表
     */
    private void insert(long key, IntList value) {
        int slot = spread(key) & mask;
        while (keys[slot] != 0L) {
            slot = (slot + 1) & mask;
        }
        keys[slot] = key;
        values[slot] = value;
        used++;
    }

    /**
     * 扩容并把所有元素重新散列。容量翻倍，保证负载因子不超过 50%。
     */
    private void grow() {
        long[] oldKeys = keys;
        IntList[] oldValues = values;
        int newCapacity = oldKeys.length << 1;
        keys = new long[newCapacity];
        values = new IntList[newCapacity];
        mask = newCapacity - 1;
        used = 0;
        for (int i = 0; i < oldKeys.length; i++) {
            if (oldKeys[i] != 0L) {
                insert(oldKeys[i], oldValues[i]);
            }
        }
    }

    /**
     * 把 64 位键混合成桶下标的高位部分，减少连续键造成的聚集。
     *
     * @param key 二元组键
     * @return 混合后的哈希值
     */
    private static int spread(long key) {
        long mixed = key * 0x9E3779B97F4A7C15L;
        return (int) (mixed >>> 33);
    }

    /** 空结果常量，避免每次查询都新建数组。 */
    private static final int[] EMPTY = new int[0];

    /**
     * 极简的可增长 int 列表，替代 {@code List<Integer>} 以减少装箱与内存开销。
     */
    static final class IntList {

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

        int get(int index) {
            return data[index];
        }

        int[] toArray() {
            int[] copy = new int[size];
            System.arraycopy(data, 0, copy, 0, size);
            return copy;
        }
    }
}
