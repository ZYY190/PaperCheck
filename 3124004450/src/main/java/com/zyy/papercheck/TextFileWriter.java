package com.zyy.papercheck;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;

/**
 * 答案文件写出器：负责输出路径校验与目录兜底创建。
 *
 * @author zyy
 */
public final class TextFileWriter {

    private static final Charset UTF_8 = Charset.forName("UTF-8");

    private TextFileWriter() {
    }

    /**
     * 把结果写入答案文件。若父目录不存在会自动创建，尽量避免因为环境差异而评测失败。
     *
     * @param path    答案文件绝对路径
     * @param content 待写入内容
     * @throws PaperCheckException 路径非法或写入失败时抛出
     */
    public static void write(String path, String content) throws PaperCheckException {
        if (path == null || path.trim().isEmpty()) {
            throw new PaperCheckException(PaperCheckException.CODE_OUTPUT_INVALID, "答案文件路径为空。");
        }
        File file = new File(path);
        if (file.isDirectory()) {
            throw new PaperCheckException(PaperCheckException.CODE_OUTPUT_INVALID,
                    "答案文件路径是一个目录: " + path);
        }
        File parent = file.getAbsoluteFile().getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new PaperCheckException(PaperCheckException.CODE_OUTPUT_INVALID,
                    "答案文件所在目录不存在且创建失败: " + parent.getPath());
        }
        try {
            Files.write(file.toPath(), content.getBytes(UTF_8));
        } catch (IOException e) {
            throw new PaperCheckException(PaperCheckException.CODE_IO_ERROR,
                    "答案文件写入失败（可能被占用或没有写权限）: " + path, e);
        }
    }
}
