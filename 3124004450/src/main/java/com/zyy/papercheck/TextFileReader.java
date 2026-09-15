package com.zyy.papercheck;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.file.Files;
import java.util.Locale;

/**
 * 文本文件读取器：负责路径校验、容量校验与编码识别。
 *
 * @author zyy
 */
public final class TextFileReader {

    /** 单个输入文件允许的最大字节数，超过则拒绝处理，避免内存溢出。 */
    public static final long MAX_FILE_BYTES = 64L * 1024 * 1024;

    private static final Charset UTF_8 = Charset.forName("UTF-8");

    private static final Charset GBK = Charset.forName("GBK");

    private TextFileReader() {
    }

    /**
     * 读取一个文本文件。
     *
     * @param path 文件绝对路径
     * @param role 该文件在业务中的角色（用于组装错误提示，如「原文文件」）
     * @return 文件内容
     * @throws PaperCheckException 路径非法、文件不存在、不可读、过大或编码无法识别时抛出
     */
    public static String read(String path, String role) throws PaperCheckException {
        File file = validate(path, role);
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            throw new PaperCheckException(PaperCheckException.CODE_IO_ERROR,
                    role + "读取失败（可能被其它程序占用）: " + path, e);
        }
        return decode(bytes, path, role);
    }

    /**
     * 校验文件路径。
     *
     * @param path 文件路径
     * @param role 文件角色
     * @return 通过校验的文件对象
     * @throws PaperCheckException 校验不通过时抛出
     */
    private static File validate(String path, String role) throws PaperCheckException {
        if (path == null || path.trim().isEmpty()) {
            throw new PaperCheckException(PaperCheckException.CODE_FILE_NOT_FOUND, role + "路径为空。");
        }
        File file = new File(path);
        if (!file.exists()) {
            throw new PaperCheckException(PaperCheckException.CODE_FILE_NOT_FOUND, role + "不存在: " + path);
        }
        if (file.isDirectory()) {
            throw new PaperCheckException(PaperCheckException.CODE_NOT_A_FILE, role + "是目录而不是文件: " + path);
        }
        if (!file.isFile()) {
            throw new PaperCheckException(PaperCheckException.CODE_NOT_A_FILE, role + "不是普通文件: " + path);
        }
        if (!file.canRead()) {
            throw new PaperCheckException(PaperCheckException.CODE_IO_ERROR, role + "不可读（权限不足）: " + path);
        }
        if (file.length() > MAX_FILE_BYTES) {
            String message = String.format(Locale.ROOT,
                    "%s 体积 %.1f MB，超过上限 %d MB: %s",
                    role, file.length() / 1048576.0,
                    MAX_FILE_BYTES / 1048576L, path);
            throw new PaperCheckException(PaperCheckException.CODE_FILE_TOO_LARGE, message);
        }
        return file;
    }

    /**
     * 解码字节内容：优先按 UTF-8 严格解码，失败后回退到 GBK。
     * 严格解码可以避免把「乱码」当成正常文本参与查重。
     *
     * @param bytes 文件字节
     * @param path  文件路径（仅用于错误提示）
     * @param role  文件角色
     * @return 解码后的文本
     * @throws PaperCheckException 两种编码都无法解码时抛出
     */
    private static String decode(byte[] bytes, String path, String role) throws PaperCheckException {
        int offset = 0;
        if (bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xEF
                && (bytes[1] & 0xFF) == 0xBB
                && (bytes[2] & 0xFF) == 0xBF) {
            offset = 3;
        }
        try {
            return decodeStrict(bytes, offset, bytes.length - offset, UTF_8);
        } catch (CharacterCodingException ignored) {
            // 不是合法的 UTF-8，继续尝试 GBK
        }
        try {
            return decodeStrict(bytes, offset, bytes.length - offset, GBK);
        } catch (CharacterCodingException e) {
            throw new PaperCheckException(PaperCheckException.CODE_ENCODING,
                    role + "编码无法识别（既不是 UTF-8 也不是 GBK）: " + path, e);
        }
    }

    /**
     * 以「遇到非法字符即报错」的方式解码一段字节。
     */
    private static String decodeStrict(byte[] bytes, int offset, int length, Charset charset)
            throws CharacterCodingException {
        CharsetDecoder decoder = charset.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        return decoder.decode(ByteBuffer.wrap(bytes, offset, length)).toString();
    }
}
