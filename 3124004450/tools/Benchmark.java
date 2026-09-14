package com.zyy.papercheck;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * 性能基准测试工具（不属于程序主体，仅用于性能分析与课程博客取证）。
 *
 * <p>对比两种实现：</p>
 * <ul>
 *   <li><b>朴素实现</b>：把两篇文章的全部有效字符直接做一次最长公共子序列比对，
 *       时间与空间复杂度均为 O(n*m)；</li>
 *   <li><b>改进实现</b>：先分句，再用二元字组倒排索引 + 命中数上界剪枝筛选候选句，
 *       最后只在候选句之间做 LCS，即 {@link SimilarityCalculator}。</li>
 * </ul>
 *
 * <p>运行：{@code java -cp build\classes com.zyy.papercheck.Benchmark}</p>
 *
 * <p>朴素实现会在独立子进程中运行并设置超时，超时即强杀，
 * 以免它继续占用 CPU 干扰后续测量。</p>
 *
 * @author zyy
 */
public final class Benchmark {

    /** 造数据用的词池，模拟中文论文中的高频词。 */
    static final String[] WORDS = {
            "软件工程", "需求分析", "体系结构", "模块划分", "接口定义", "单元测试", "集成测试", "系统测试",
            "覆盖率", "回归测试", "重构", "版本控制", "持续集成", "代码评审", "性能优化", "内存占用",
            "算法复杂度", "数据结构", "设计模式", "可读性", "可维护性", "可测试性", "需求规格", "说明书",
            "开发效率", "维护成本", "团队协作", "项目管理", "风险控制", "质量保证"
    };

    /** 朴素实现的时间上限，超过则记为「超时」。 */
    private static final long NAIVE_CAP_MILLIS = 30000L;

    private Benchmark() {
    }

    /**
     * @param args 无参数时执行完整对比；{@code --naive <字符数>} 时只运行朴素实现并输出「耗时,重复率」
     */
    public static void main(String[] args) {
        if (args != null && args.length == 2 && "--naive".equals(args[0])) {
            runNaiveOnly(Integer.parseInt(args[1]));
            return;
        }
        runComparison();
    }

    /**
     * 子进程模式：只跑一次朴素实现。
     *
     * @param size 文本规模（字符数）
     */
    private static void runNaiveOnly(int size) {
        String[] pair = generate(size, SEED_BASE + size);
        long start = System.nanoTime();
        double rate = naive(pair[0], pair[1]);
        long millis = (System.nanoTime() - start) / 1000000L;
        System.out.println(millis + "," + String.format(Locale.ROOT, "%.4f", Double.valueOf(rate)));
    }

    /**
     * 主模式：逐个规模对比两种实现。
     */
    private static void runComparison() {
        int[] sizes = {10000, 50000, 100000, 200000, 500000, 1000000};
        System.out.println("文本规模(字符),朴素整篇比对(ms),分句+索引剪枝(ms),重复率");
        String javaExecutable = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        String classPath = System.getProperty("java.class.path");
        for (int i = 0; i < sizes.length; i++) {
            String[] pair = generate(sizes[i], SEED_BASE + sizes[i]);
            String original = pair[0];
            String copied = pair[1];
            String naive = runNaiveInChildProcess(javaExecutable, classPath, sizes[i]);
            long fast = timedFast(original, copied);
            double rate = new SimilarityCalculator().calculate(original, copied);
            System.out.println(String.format(Locale.ROOT,
                    "%d,%s,%d,%s",
                    Integer.valueOf(sizes[i]),
                    naive,
                    Long.valueOf(fast),
                    String.format(Locale.ROOT, "%.4f", Double.valueOf(rate))));
        }
    }

    /**
     * 朴素的整篇比对实现。
     *
     * @param original 原文
     * @param copied   抄袭版
     * @return 重复率
     */
    private static double naive(String original, String copied) {
        int[] left = TextNormalizer.toFeatureCodePoints(TextNormalizer.normalize(original));
        int[] right = TextNormalizer.toFeatureCodePoints(TextNormalizer.normalize(copied));
        if (left.length == 0 || right.length == 0) {
            return 0.0;
        }
        int common = new LcsMatcher(Math.min(left.length, right.length)).length(left, right);
        return (double) common / right.length;
    }

    /**
     * 在独立子进程中运行朴素实现，超时即强杀。
     *
     * @param javaExecutable java 可执行文件路径
     * @param classPath      类路径
     * @param size           文本规模
     * @return 耗时毫秒的字符串；超时返回「&gt;30000」
     */
    private static String runNaiveInChildProcess(String javaExecutable, String classPath, int size) {
        ProcessBuilder builder = new ProcessBuilder(javaExecutable, "-cp", classPath,
                "com.zyy.papercheck.Benchmark", "--naive", String.valueOf(size));
        builder.redirectErrorStream(true);
        Process process = null;
        try {
            process = builder.start();
            if (!process.waitFor(NAIVE_CAP_MILLIS, TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                return ">" + NAIVE_CAP_MILLIS;
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"));
            String line = reader.readLine();
            reader.close();
            if (line == null) {
                return "error";
            }
            return line.split(",")[0];
        } catch (Exception e) {
            Thread.currentThread().interrupt();
            return "error";
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    /** 随机种子基数，保证每次运行生成的测试文本完全一致。 */
    private static final long SEED_BASE = 20240915L;

    /**
     * 测量改进实现的耗时。
     *
     * @param original 原文
     * @param copied   抄袭版
     * @return 耗时毫秒
     */
    private static long timedFast(String original, String copied) {
        SimilarityCalculator calculator = new SimilarityCalculator();
        long start = System.nanoTime();
        calculator.calculate(original, copied);
        return (System.nanoTime() - start) / 1000000L;
    }

    /**
     * 生成一对「有一定重复但不完全相同」的测试文本。
     *
     * @param targetChars 目标字符数
     * @param seed        随机种子，保证结果可复现
     * @return 长度为 2 的数组：原文、抄袭版
     */
    static String[] generate(int targetChars, long seed) {
        Random random = new Random(seed);
        StringBuilder original = new StringBuilder(targetChars + 256);
        StringBuilder copied = new StringBuilder(targetChars + 256);
        while (original.length() < targetChars) {
            String sentence = randomSentence(random);
            original.append(sentence);
            int roll = random.nextInt(100);
            if (roll < 10) {
                copied.append(randomSentence(random));
            } else if (roll < 30) {
                copied.append(reword(sentence, random));
            } else {
                copied.append(sentence);
            }
        }
        return new String[] {original.toString(), copied.toString()};
    }

    /**
     * @param random 随机源
     * @return 一条随机中文句子
     */
    private static String randomSentence(Random random) {
        int wordCount = 8 + random.nextInt(7);
        StringBuilder sentence = new StringBuilder();
        for (int i = 0; i < wordCount; i++) {
            sentence.append(WORDS[random.nextInt(WORDS.length)]);
            if (i % 5 == 4) {
                sentence.append('，');
            }
        }
        sentence.append('。');
        return sentence.toString();
    }

    /**
     * 把句子中的一个词替换成另一个词，模拟「改」这一类抄袭。
     *
     * @param sentence 原句
     * @param random   随机源
     * @return 改写后的句子
     */
    private static String reword(String sentence, Random random) {
        String from = WORDS[random.nextInt(WORDS.length)];
        String to = WORDS[random.nextInt(WORDS.length)];
        if (from.equals(to) || sentence.indexOf(from) < 0) {
            return sentence;
        }
        return Pattern.compile(Pattern.quote(from)).matcher(sentence).replaceFirst(to);
    }
}
