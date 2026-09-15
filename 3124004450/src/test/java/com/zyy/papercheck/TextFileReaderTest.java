package com.zyy.papercheck;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * {@link TextFileReader} 的单元测试：验证路径校验与编码识别。
 */
class TextFileReaderTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("文件不存在时抛出 E002")
    void missingFileThrowsFileNotFound() {
        PaperCheckException exception = assertThrows(PaperCheckException.class,
                () -> TextFileReader.read(tempDir.resolve("not-exist.txt").toString(), "原文文件"));
        assertEquals(PaperCheckException.CODE_FILE_NOT_FOUND, exception.getCode());
    }

    @Test
    @DisplayName("路径指向目录时抛出 E003")
    void directoryThrowsNotAFile() {
        PaperCheckException exception = assertThrows(PaperCheckException.class,
                () -> TextFileReader.read(tempDir.toString(), "原文文件"));
        assertEquals(PaperCheckException.CODE_NOT_A_FILE, exception.getCode());
    }

    @Test
    @DisplayName("路径为空时抛出 E002")
    void emptyPathThrows() {
        assertEquals(PaperCheckException.CODE_FILE_NOT_FOUND,
                assertThrows(PaperCheckException.class, () -> TextFileReader.read("   ", "原文文件")).getCode());
        assertEquals(PaperCheckException.CODE_FILE_NOT_FOUND,
                assertThrows(PaperCheckException.class, () -> TextFileReader.read(null, "原文文件")).getCode());
    }

    @Test
    @DisplayName("UTF-8 BOM 会被自动去掉")
    void utf8BomIsStripped() throws Exception {
        Path file = tempDir.resolve("bom.txt");
        byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] body = "论文查重".getBytes(Charset.forName("UTF-8"));
        byte[] all = new byte[bom.length + body.length];
        System.arraycopy(bom, 0, all, 0, bom.length);
        System.arraycopy(body, 0, all, bom.length, body.length);
        Files.write(file, all);
        assertEquals("论文查重", TextFileReader.read(file.toString(), "原文文件"));
    }

    @Test
    @DisplayName("GBK 编码的文件可以被正确解码")
    void gbkContentIsDecoded() throws Exception {
        Path file = tempDir.resolve("gbk.txt");
        Files.write(file, "今天天气很好".getBytes(Charset.forName("GBK")));
        assertEquals("今天天气很好", TextFileReader.read(file.toString(), "原文文件"));
    }

    @Test
    @DisplayName("文件超过容量上限时抛出 E006，避免内存溢出")
    void tooLargeFileThrows() throws Exception {
        Path file = tempDir.resolve("huge.txt");
        // 直接把文件长度撑大，不实际占用磁盘空间
        try (java.io.RandomAccessFile handle = new java.io.RandomAccessFile(file.toFile(), "rw")) {
            handle.setLength(TextFileReader.MAX_FILE_BYTES + 1);
        }
        PaperCheckException exception = assertThrows(PaperCheckException.class,
                () -> TextFileReader.read(file.toString(), "原文文件"));
        assertEquals(PaperCheckException.CODE_FILE_TOO_LARGE, exception.getCode());
    }

    @Test
    @DisplayName("既不是 UTF-8 也不是 GBK 的字节流抛出 E005")
    void unknownEncodingThrows() throws Exception {
        Path file = tempDir.resolve("broken.txt");
        Files.write(file, new byte[] {(byte) 0x80, (byte) 0x80, (byte) 0xFE});
        PaperCheckException exception = assertThrows(PaperCheckException.class,
                () -> TextFileReader.read(file.toString(), "原文文件"));
        assertEquals(PaperCheckException.CODE_ENCODING, exception.getCode());
    }
}
