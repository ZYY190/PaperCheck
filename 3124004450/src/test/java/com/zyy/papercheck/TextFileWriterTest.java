package com.zyy.papercheck;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * {@link TextFileWriter} 的单元测试：验证写出内容与目录兜底逻辑。
 */
class TextFileWriterTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("写出的内容与预期完全一致，不添加多余字符")
    void writesContentExactly() throws Exception {
        Path file = tempDir.resolve("answer.txt");
        TextFileWriter.write(file.toString(), "0.88");
        assertEquals("0.88", new String(Files.readAllBytes(file), Charset.forName("UTF-8")));
    }

    @Test
    @DisplayName("父目录不存在时会自动创建")
    void createsMissingParentDirectory() throws Exception {
        Path file = tempDir.resolve("nested/deeper/answer.txt");
        TextFileWriter.write(file.toString(), "1.00");
        assertTrue(Files.exists(file));
    }

    @Test
    @DisplayName("目标是目录时抛出 E007")
    void directoryAsTargetThrows() {
        PaperCheckException exception = assertThrows(PaperCheckException.class,
                () -> TextFileWriter.write(tempDir.toString(), "0.50"));
        assertEquals(PaperCheckException.CODE_OUTPUT_INVALID, exception.getCode());
    }

    @Test
    @DisplayName("路径为空时抛出 E007")
    void emptyPathThrows() {
        assertEquals(PaperCheckException.CODE_OUTPUT_INVALID,
                assertThrows(PaperCheckException.class, () -> TextFileWriter.write(null, "0.50")).getCode());
    }

    @Test
    @DisplayName("覆盖已存在的答案文件")
    void overwritesExistingAnswerFile() throws Exception {
        Path file = tempDir.resolve("answer.txt");
        Files.write(file, "旧内容".getBytes(Charset.forName("UTF-8")));
        TextFileWriter.write(file.toString(), "0.33");
        assertEquals("0.33", new String(Files.readAllBytes(file), Charset.forName("UTF-8")));
    }
}
