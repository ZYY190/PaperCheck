package com.zyy.papercheck;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 计算模块核心：重复率计算器。
 *
 * <p><b>算法思想</b>：把「重复率」定义为「抄袭版中有多少比例的内容，可以在原文里找到顺序一致的对应」。
 * 该定义天然是单向的——抄袭版里新写的内容不算重复，抄来的内容才算重复，与查重业务语义一致。</p>
 *
 * <p><b>计算步骤</b>：</p>
 * <ol>
 *   <li>规范化：消除全角/半角、大小写、不可见字符等无关差异；</li>
 *   <li>分句：把文章切成句子，使单次比对的规模有上界；</li>
 *   <li>建立原文的二元字组倒排索引；</li>
 *   <li>对抄袭版的每一句，用倒排索引筛出候选句，再用 LCS 求最大公共子序列长度；</li>
 *   <li>重复率 = 各句最大公共子序列长度之和 / 抄袭版特征字符总数。</li>
 * </ol>
 *
 * <p><b>性能设计</b>：候选筛选与命中计数全部使用原始类型数组，配合「时间戳数组」
 * 代替每次清零，避免在大文件上产生海量临时对象；对完全相同的句子直接短路返回，
 * 不再做 LCS。这些措施使百万字符级别的文档也能在一秒量级完成。</p>
 *
 * <p>本类无状态，可安全地在多线程中共享使用。</p>
 *
 * @author zyy
 */
public final class SimilarityCalculator {

    /** 每个句子最多保留的候选句数量，防止热门句子拖慢整体速度。 */
    static final int MAX_CANDIDATES = 512;

    /** 每个句子最多做多少次 LCS 精确比对，保证单句开销有硬上界。 */
    static final int MAX_LCS_PER_SENTENCE = 24;

    /** 跨句匹配时最多合并的原文句数。 */
    static final int MAX_MERGE_SPAN = 3;

    /** 句子长度相差超过该倍数时直接判定为不可能高度重复。 */
    static final int LENGTH_RATIO = 6;

    /** 高频字组的词频上限下限：出现在超过该数量句子里的字组不参与候选筛选。 */
    static final int POSTINGS_DF_MIN = 64;

    /** 单个句子扫描倒排表的最大条目数，保证单句开销有硬上界。 */
    static final int MAX_POSTINGS_VISITS = 4000;

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
            // 快速路径：完全相同的文件无需分句比对；
            // 但纯空白/纯标点的「相同」并没有任何有效内容，仍然应当返回 0
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

    /**
     * 统计句子列表中的特征字符总数。
     *
     * @param sentences 句子列表
     * @return 特征字符总数
     */
    private static int countCodePoints(List<int[]> sentences) {
        int total = 0;
        for (int i = 0; i < sentences.size(); i++) {
            total += sentences.get(i).length;
        }
        return total;
    }

    /**
     * 逐句匹配，返回抄袭版中被判为重复的字符总数。
     *
     * @param original 原文句子
     * @param copied   抄袭版句子
     * @return 重复字符数
     */
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
        int sentenceCount = original.size();
        // 命中计数与时间戳数组：用递增的时间戳代替「每次查询都清零」，避免 O(句子数) 的重复初始化
        int[] hitCount = new int[sentenceCount];
        int[] stamp = new int[sentenceCount];
        CandidateBag candidates = new CandidateBag();
        // 字组的词频上限随文档规模自适应：文档越大，「常见字组」的判定阈值越高，
        // 从而在保持召回率的同时过滤掉区分度极低的字组
        int dfLimit = Math.max(POSTINGS_DF_MIN, sentenceCount / 50);

        LcsMatcher matcher = new LcsMatcher(Math.max(maxOriginalLength, maxLength(copied)));
        // 复用的缓冲区：避免每处理一句都新建数组
        int[] orderBuffer = new int[SentenceSegmenter.MAX_SENTENCE_LENGTH];
        int[] frequencyBuffer = new int[SentenceSegmenter.MAX_SENTENCE_LENGTH];
        int[] sortedBuffer = new int[MAX_CANDIDATES];
        int[] bucketBuffer = new int[SentenceSegmenter.MAX_SENTENCE_LENGTH + 2];
        int[] startBuffer = new int[SentenceSegmenter.MAX_SENTENCE_LENGTH + 2];
        int[] mergeBuffer = new int[MAX_MERGE_SPAN * SentenceSegmenter.MAX_SENTENCE_LENGTH];
        int matched = 0;
        int stampCounter = 0;
        for (int i = 0; i < copied.size(); i++) {
            stampCounter++;
            matched += bestMatch(copied.get(i), original, index, matcher, originalSingleChars,
                    hitCount, stamp, stampCounter, dfLimit, candidates,
                    orderBuffer, frequencyBuffer, sortedBuffer, bucketBuffer, startBuffer, mergeBuffer);
        }
        return matched;
    }

    /**
     * 在原文中为抄袭版的一个句子寻找最匹配的句子。
     *
     * @param sentence            抄袭版句子
     * @param original            原文句子
     * @param index               原文的二元字组倒排索引
     * @param matcher             可复用的 LCS 计算器
     * @param originalSingleChars 原文中出现过的单字集合，用于处理长度小于 2 的句子
     * @param hitCount            命中计数数组（与 stamp 配合使用）
     * @param stamp               时间戳数组，标记某个候选是否属于本轮
     * @param stampCounter        本轮时间戳
     * @param dfLimit             本轮允许的字组最大词频
     * @param candidates          复用的候选集合
     * @param orderBuffer         复用的字组顺序缓冲区
     * @param frequencyBuffer     复用的字组词频缓冲区
     * @param sortedBuffer        复用的候选排序结果缓冲区
     * @param bucketBuffer        复用的计数排序桶
     * @param startBuffer         复用的计数排序起始位置数组
     * @param mergeBuffer         跨句匹配时用于拼接原文句的缓冲区
     * @return 该句被判为重复的字符数
     */
    private int bestMatch(int[] sentence, List<int[]> original, NGramIndex index,
                          LcsMatcher matcher, Set<Integer> originalSingleChars,
                          int[] hitCount, int[] stamp, int stampCounter, int dfLimit,
                          CandidateBag candidates, int[] orderBuffer, int[] frequencyBuffer,
                          int[] sortedBuffer, int[] bucketBuffer, int[] startBuffer,
                          int[] mergeBuffer) {
        int length = sentence.length;
        if (length <= 0) {
            return 0;
        }
        if (length == 1) {
            return originalSingleChars.contains(Integer.valueOf(sentence[0])) ? 1 : 0;
        }
        int totalGrams = length - 1;
        candidates.reset();
        int known = collect(sentence, index, hitCount, stamp, stampCounter, dfLimit, candidates,
                orderBuffer, frequencyBuffer);
        if (candidates.size == 0) {
            // 高频字组被过滤后没有候选，退化为不过滤再试一次
            candidates.reset();
            known = collect(sentence, index, hitCount, stamp, stampCounter, Integer.MAX_VALUE, candidates,
                    orderBuffer, frequencyBuffer);
        }
        if (candidates.size == 0) {
            return 0;
        }
        // 尚未扫描到的字组最多还能为某个候选再贡献 unknown 次命中，
        // 据此得到严格成立的上界：LCS <= 命中数 + 未统计字组数 + 1
        int unknown = totalGrams - known;
        if (unknown < 0) {
            unknown = 0;
        }
        orderByHitsDescending(candidates, hitCount, sortedBuffer, bucketBuffer, startBuffer);
        int best = 0;
        int bestId = -1;
        int examined = 0;
        for (int k = 0; k < candidates.size && examined < MAX_LCS_PER_SENTENCE; k++) {
            int id = sortedBuffer[k];
            int[] candidate = original.get(id);
            int minLength = Math.min(candidate.length, length);
            // 候选已按命中数降序排列，后面的命中数只会更小，因此这里可以直接终止
            int upperBound = Math.min(minLength, hitCount[id] + unknown + 1);
            if (upperBound <= best) {
                break;
            }
            if (candidate.length > (long) length * LENGTH_RATIO
                    || length > (long) candidate.length * LENGTH_RATIO) {
                continue;
            }
            examined++;
            int value;
            if (candidate.length == length && Arrays.equals(candidate, sentence)) {
                // 完全相同的句子直接短路，省掉一整次 LCS
                value = length;
            } else if ((long) candidate.length * length > LcsMatcher.MAX_CELLS) {
                // 规模过大时退化为上界估计，保证不会因单次比较而超时
                value = upperBound;
            } else {
                value = matcher.length(sentence, candidate);
            }
            if (value > best) {
                best = value;
                bestId = id;
                if (best == minLength) {
                    // 已经完全包含，无需继续比较
                    break;
                }
            }
        }
        if (bestId >= 0 && best < length) {
            // 删改可能让抄袭句跨越原文的句子边界（例如删掉半句后前后两句被拼到了一起），
            // 此时单句比对只能匹配到一部分，需要把相邻的原文句合并起来再比一次。
            best = Math.max(best, matchMerged(sentence, original, bestId, matcher, mergeBuffer, best));
        }
        return best;
    }

    /**
     * 把最佳候选句与其相邻的原文句合并后重新比对，处理「抄袭句跨越原文句边界」的情况。
     *
     * <p>只在单句比对没能完全匹配时才调用，因此对绝大多数句子没有额外开销。</p>
     *
     * @param sentence   抄袭版句子
     * @param original   原文句子
     * @param center     最佳候选句的编号
     * @param matcher    可复用的 LCS 计算器
     * @param buffer     拼接缓冲区
     * @param currentBest 当前的匹配长度
     * @return 合并若干相邻句之后能取得的最大匹配长度
     */
    private int matchMerged(int[] sentence, List<int[]> original, int center,
                            LcsMatcher matcher, int[] buffer, int currentBest) {
        int best = currentBest;
        int size = original.size();
        for (int span = 2; span <= MAX_MERGE_SPAN; span++) {
            for (int start = center - span + 1; start <= center; start++) {
                if (start < 0 || start + span > size) {
                    continue;
                }
                int total = 0;
                for (int k = start; k < start + span; k++) {
                    total += original.get(k).length;
                }
                if (total > buffer.length) {
                    continue;
                }
                int position = 0;
                for (int k = start; k < start + span; k++) {
                    int[] part = original.get(k);
                    System.arraycopy(part, 0, buffer, position, part.length);
                    position += part.length;
                }
                int value = matcher.length(sentence, Arrays.copyOf(buffer, position));
                if (value > best) {
                    best = value;
                    if (best == sentence.length) {
                        return best;
                    }
                }
            }
        }
        return best;
    }

    /**
     * 统计抄袭版句子在原文各句中的命中字组数。
     *
     * @param sentence     抄袭版句子
     * @param index        原文倒排索引
     * @param hitCount     命中计数数组
     * @param stamp        时间戳数组
     * @param stampCounter 本轮时间戳
     * @param dfLimit      允许的字组最大词频
     * @param candidates   候选集合，首次命中的句子编号会被追加进来
     * @param orderBuffer  复用的字组顺序缓冲区
     * @param frequencyBuffer 复用的字组词频缓冲区
     * @return 已经「完整扫描过倒排表」的字组数量；它决定了上界的松紧程度
     */
    private int collect(int[] sentence, NGramIndex index, int[] hitCount, int[] stamp,
                        int stampCounter, int dfLimit, CandidateBag candidates,
                        int[] orderBuffer, int[] frequencyBuffer) {
        // 第一步：统计每个字组的词频，并按词频升序排序。
        // 稀有字组（如「需求规格」）的倒排表很短，命中它的句子极可能就是重复源；
        // 常见字组（如「的是」）的倒排表很长，既慢又几乎没有区分度。
        // 按词频升序处理，可以让有限的访问预算优先花在高价值特征上。
        int totalGrams = sentence.length - 1;
        int gramCount = 0;
        // 倒排表中不存在的字组是「已知不含」的，可以直接算作已扫描
        int known = 0;
        for (int i = 0; i < totalGrams; i++) {
            NGramIndex.IntList postings = index.postingsOf(sentence[i], sentence[i + 1]);
            if (postings == null) {
                known++;
                continue;
            }
            int postingSize = postings.size();
            if (postingSize > dfLimit) {
                continue;
            }
            int position = gramCount;
            while (position > 0 && frequencyBuffer[position - 1] > postingSize) {
                frequencyBuffer[position] = frequencyBuffer[position - 1];
                orderBuffer[position] = orderBuffer[position - 1];
                position--;
            }
            frequencyBuffer[position] = postingSize;
            orderBuffer[position] = i;
            gramCount++;
        }

        // 第二步：按排好的顺序累积候选句与命中次数
        int visits = 0;
        for (int g = 0; g < gramCount; g++) {
            int i = orderBuffer[g];
            NGramIndex.IntList postings = index.postingsOf(sentence[i], sentence[i + 1]);
            int postingSize = postings.size();
            if (visits + postingSize > MAX_POSTINGS_VISITS) {
                // 预算不足时不再扫描，也不把该字组算作已扫描，保证上界依然成立
                return known;
            }
            visits += postingSize;
            for (int k = 0; k < postingSize; k++) {
                int id = postings.get(k);
                if (stamp[id] != stampCounter) {
                    stamp[id] = stampCounter;
                    hitCount[id] = 0;
                    if (candidates.size < MAX_CANDIDATES) {
                        candidates.add(id);
                    }
                }
                hitCount[id]++;
            }
            known++;
        }
        return known;
    }

    /**
     * 用计数排序把候选句按命中数从大到小排列。
     *
     * <p>命中数一定不超过句子的字组总数，取值范围很小，因此计数排序是线性的，
     * 比通用的比较排序更快，也不产生任何临时对象。</p>
     *
     * @param candidates  候选集合
     * @param hitCount    命中计数数组
     * @param sorted      输出缓冲区，存放排好序的候选编号
     * @param bucket      计数桶缓冲区
     * @param start       起始位置缓冲区
     */
    private void orderByHitsDescending(CandidateBag candidates, int[] hitCount,
                                       int[] sorted, int[] bucket, int[] start) {
        int[] ids = candidates.ids;
        int size = candidates.size;
        int maxHits = 0;
        for (int k = 0; k < size; k++) {
            int hits = hitCount[ids[k]];
            bucket[hits]++;
            if (hits > maxHits) {
                maxHits = hits;
            }
        }
        int position = 0;
        for (int hits = maxHits; hits >= 0; hits--) {
            start[hits] = position;
            position += bucket[hits];
        }
        for (int k = 0; k < size; k++) {
            int id = ids[k];
            int hits = hitCount[id];
            sorted[start[hits]++] = id;
        }
        // 清空计数桶，供下一个句子复用
        Arrays.fill(bucket, 0, maxHits + 1, 0);
    }

    /**
     * @param sentences 句子列表
     * @return 最长句子的特征字符数
     */
    private static int maxLength(List<int[]> sentences) {
        int max = 1;
        for (int i = 0; i < sentences.size(); i++) {
            if (sentences.get(i).length > max) {
                max = sentences.get(i).length;
            }
        }
        return max;
    }

    /**
     * 复用的候选句集合。用可增长数组代替 {@code ArrayList<Integer>}，避免装箱与反复分配。
     */
    private static final class CandidateBag {

        private int[] ids = new int[16];
        private int size;

        void reset() {
            size = 0;
        }

        void add(int id) {
            if (size == ids.length) {
                ids = Arrays.copyOf(ids, ids.length * 2);
            }
            ids[size++] = id;
        }
    }
}
