package com.zyy.papercheck;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link NGramIndex} 的单元测试：验证倒排索引的登记与查询。
 */
class NGramIndexTest {

    @Test
    @DisplayName("能够查到包含指定字组的句子编号")
    void lookupFindsSentenceIds() {
        NGramIndex index = new NGramIndex();
        index.add(0, codes("今天天气"));
        index.add(1, codes("天气晴朗"));
        assertArrayEquals(new int[] {0, 1}, index.lookup(codes("天气")[0], codes("天气")[1]));
    }

    @Test
    @DisplayName("同一个句子内重复出现的字组只登记一次")
    void duplicatedGramInSameSentenceIsIndexedOnce() {
        NGramIndex index = new NGramIndex();
        index.add(7, codes("天天天天"));
        assertArrayEquals(new int[] {7}, index.lookup(codes("天天")[0], codes("天天")[1]));
    }

    @Test
    @DisplayName("查询不存在的字组返回空数组而不是 null")
    void unknownGramReturnsEmptyArray() {
        NGramIndex index = new NGramIndex();
        index.add(0, codes("今天天气"));
        assertEquals(0, index.lookup(codes("银河")[0], codes("银河")[1]).length);
    }

    @Test
    @DisplayName("字组总数等于不同字组的个数")
    void gramCountReflectsDistinctGrams() {
        NGramIndex index = new NGramIndex();
        index.add(0, codes("abc"));
        assertEquals(2, index.gramCount());
        index.add(1, codes("abc"));
        assertEquals(2, index.gramCount());
    }

    @Test
    @DisplayName("字组数量超过初始容量时能够自动扩容并保持查询正确")
    void growsWhenManyGramsAreInserted() {
        NGramIndex index = new NGramIndex();
        // 构造大量互不相同的字组，触发多轮扩容
        for (int i = 0; i < 20000; i++) {
            int first = 0x4E00 + i / 128;
            int second = 0x4E00 + i % 128;
            index.add(i % 500, new int[] {first, second});
        }
        assertEquals(20000, index.gramCount());
        // 扩容后仍需命中正确的句子编号
        int probe = 123;
        int[] hit = index.lookup(0x4E00 + probe / 128, 0x4E00 + probe % 128);
        assertEquals(1, hit.length);
        assertEquals(probe % 500, hit[0]);
    }

    @Test
    @DisplayName("同一个字组被多个句子引用时按句子顺序返回")
    void postingsAreOrderedBySentenceId() {
        NGramIndex index = new NGramIndex();
        for (int id = 0; id < 50; id++) {
            index.add(id, codes("论文查重算法"));
        }
        int[] first = index.lookup(codes("论文")[0], codes("论文")[1]);
        assertEquals(50, first.length);
        assertEquals(0, first[0]);
        assertEquals(49, first[49]);
    }

    private static int[] codes(String text) {
        return TextNormalizer.toFeatureCodePoints(TextNormalizer.normalize(text));
    }
}
