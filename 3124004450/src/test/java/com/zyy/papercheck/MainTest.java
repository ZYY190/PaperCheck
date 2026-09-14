package com.zyy.papercheck;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * {@link Main} 的单元测试：验证命令行入口的参数校验、退出码与答案文件格式。
 */
class MainTest {

    private static final Charset UTF_8 = Charset.forName("UTF-8");

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("参数个数不对时返回退出码 2 并打印用法")
    void wrongArgumentCountReturnsUsageError() {
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        int exitCode = new Main().run(new String[] {"a.txt"}, new PrintStream(new ByteArrayOutputStream()),
                new PrintStream(err));
        assertEquals(2, exitCode);
        assertTrue(err.toString().contains(Main.USAGE));
    }

    @Test
    @DisplayName("输入文件不存在时返回退出码 3")
    void missingInputFileReturnsBusinessError() {
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        Path answer = tempDir.resolve("ans.txt");
        int exitCode = new Main().run(new String[] {
                tempDir.resolve("no-orig.txt").toString(),
                tempDir.resolve("no-copy.txt").toString(),
                answer.toString()}, new PrintStream(new ByteArrayOutputStream()), new PrintStream(err));
        assertEquals(3, exitCode);
        assertTrue(err.toString().contains(PaperCheckException.CODE_FILE_NOT_FOUND));
    }

    @Test
    @DisplayName("正常运行时答案文件内容为保留两位小数的浮点数")
    void fullRunWritesAnswerFileWithTwoDecimals() throws IOException {
        Path original = tempDir.resolve("orig.txt");
        Path copied = tempDir.resolve("orig_add.txt");
        Path answer = tempDir.resolve("answer.txt");
        Files.write(original, "今天是星期天，天气晴，今天晚上我要去看电影。".getBytes(UTF_8));
        Files.write(copied, "今天是周天，天气晴朗，我晚上要去看电影。".getBytes(UTF_8));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int exitCode = new Main().run(new String[] {
                original.toString(), copied.toString(), answer.toString()},
                new PrintStream(out), new PrintStream(new ByteArrayOutputStream()));

        assertEquals(0, exitCode);
        String content = new String(Files.readAllBytes(answer), UTF_8);
        assertTrue(content.matches("\\d+\\.\\d{2}"), "答案格式不正确: " + content);
        assertEquals(content, out.toString().trim());
    }

    @Test
    @DisplayName("答案路径不可写时返回退出码 3 并给出 E007")
    void unwritableAnswerPathReturnsBusinessError() throws IOException {
        Path original = tempDir.resolve("orig2.txt");
        Path copied = tempDir.resolve("copy2.txt");
        Files.write(original, "论文查重算法测试文本。".getBytes(UTF_8));
        Files.write(copied, "论文查重算法测试文本。".getBytes(UTF_8));

        ByteArrayOutputStream err = new ByteArrayOutputStream();
        int exitCode = new Main().run(new String[] {
                original.toString(), copied.toString(), tempDir.toString()},
                new PrintStream(new ByteArrayOutputStream()), new PrintStream(err));

        assertEquals(3, exitCode);
        assertTrue(err.toString().contains(PaperCheckException.CODE_OUTPUT_INVALID));
    }

    @Test
    @DisplayName("空文件不会导致异常退出，重复率为 0.00")
    void emptyFilesProduceZero() throws IOException {
        Path original = tempDir.resolve("empty-orig.txt");
        Path copied = tempDir.resolve("empty-copy.txt");
        Path answer = tempDir.resolve("empty-answer.txt");
        Files.write(original, new byte[0]);
        Files.write(copied, new byte[0]);
        int exitCode = new Main().run(new String[] {
                original.toString(), copied.toString(), answer.toString()},
                new PrintStream(new ByteArrayOutputStream()), new PrintStream(new ByteArrayOutputStream()));
        assertEquals(0, exitCode);
        assertEquals("0.00", new String(Files.readAllBytes(answer), UTF_8));
    }
}
