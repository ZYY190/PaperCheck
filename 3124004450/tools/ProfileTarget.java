package com.zyy.papercheck;

import java.util.Locale;

/**
 * 性能采样用的负载程序（不属于程序主体）。
 *
 * <p>在 Java Flight Recorder（或 VisualVM / JProfiler）下运行本类，
 * 即可得到计算模块中各函数的 CPU 占用分布，用于定位性能瓶颈。</p>
 *
 * <p>运行示例：</p>
 * <pre>
 * java -XX:+UnlockCommercialFeatures -XX:+FlightRecorder ^
 *      -XX:StartFlightRecording=duration=45s,filename=build\perf.jfr,settings=profile ^
 *      -cp build\classes com.zyy.papercheck.ProfileTarget 1000000 20
 * </pre>
 *
 * @author zyy
 */
public final class ProfileTarget {

    private ProfileTarget() {
    }

    /**
     * @param args 可选：字符数（默认 50 万）、重复轮数（默认 20）
     */
    public static void main(String[] args) {
        int size = args.length > 0 ? Integer.parseInt(args[0]) : 500000;
        int rounds = args.length > 1 ? Integer.parseInt(args[1]) : 20;
        String[] pair = Benchmark.generate(size, 20240915L + size);
        SimilarityCalculator calculator = new SimilarityCalculator();
        long start = System.nanoTime();
        for (int i = 0; i < rounds; i++) {
            double rate = calculator.calculate(pair[0], pair[1]);
            if (i == 0) {
                System.out.println(String.format(Locale.ROOT, "重复率=%.4f", Double.valueOf(rate)));
            }
        }
        long millis = (System.nanoTime() - start) / 1000000L;
        System.out.println(String.format(Locale.ROOT, "共 %d 轮，总耗时 %d ms，平均 %d ms/轮",
                Integer.valueOf(rounds), Long.valueOf(millis), Long.valueOf(millis / rounds)));
    }
}
