package com.zyy.papercheck;

import java.io.PrintStream;
import java.util.Locale;

/**
 * 程序入口。
 *
 * <p>命令行格式（三个参数均使用绝对路径，路径中不允许出现空格）：</p>
 * <pre>
 * java -jar main.jar &lt;原文文件&gt; &lt;抄袭版论文文件&gt; &lt;答案文件&gt;
 * </pre>
 *
 * <p>退出码约定：0 成功；2 参数个数错误；3 可预期的业务错误；4 未预期的内部错误。</p>
 *
 * @author zyy
 */
public final class Main {

    /** 命令行用法提示。 */
    static final String USAGE =
            "用法: java -jar main.jar <原文文件> <抄袭版论文文件> <答案文件>";

    /**
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        int exitCode = new Main().run(args, System.out, System.err);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    /**
     * 执行一次完整的查重流程。抽出该方法是为了让单元测试可以直接断言退出码与输出，
     * 而不必反复启动 JVM 子进程。
     *
     * @param args 命令行参数
     * @param out  正常输出流
     * @param err  错误输出流
     * @return 进程退出码
     */
    public int run(String[] args, PrintStream out, PrintStream err) {
        if (args == null || args.length != 3) {
            err.println("[错误 " + PaperCheckException.CODE_ARGUMENT_COUNT + "] 参数个数不正确，需要 3 个文件路径。");
            err.println(USAGE);
            return 2;
        }
        try {
            String original = TextFileReader.read(args[0], "原文文件");
            String copied = TextFileReader.read(args[1], "抄袭版论文文件");
            double rate = new SimilarityCalculator().calculate(original, copied);
            String answer = String.format(Locale.ROOT, "%.2f", Double.valueOf(rate));
            TextFileWriter.write(args[2], answer);
            out.println(answer);
            return 0;
        } catch (PaperCheckException e) {
            err.println("[错误 " + e.getCode() + "] " + e.getMessage());
            return 3;
        } catch (RuntimeException e) {
            err.println("[错误 E500] 程序内部错误: " + e.getMessage());
            return 4;
        }
    }
}
