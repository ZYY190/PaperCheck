package com.zyy.papercheck;

import java.util.Arrays;

/**
 * 文本规范化工具。
 *
 * <p>查重算法对「同一个字的不同写法」应当一视同仁，因此在进入算法核心之前，
 * 先把原始文本统一成标准形式，消除与内容无关的差异。</p>
 *
 * <p>类设计为不可变、无状态，所有方法都是静态的，便于单元测试。</p>
 *
 * @author zyy
 */
public final class TextNormalizer {

    /** 全角 ASCII 区间起点（！）。 */
    private static final int FULL_WIDTH_START = 0xFF01;
    /** 全角 ASCII 区间终点（～）。 */
    private static final int FULL_WIDTH_END = 0xFF5E;
    /** 全角与半角之间的固定偏移量。 */
    private static final int FULL_WIDTH_OFFSET = 0xFEE0;
    /** 全角空格。 */
    private static final int IDEOGRAPHIC_SPACE = 0x3000;

    private TextNormalizer() {
    }

    /**
     * 把原始文本统一成标准文本，规则如下：
     * <ol>
     *   <li>去掉 UTF-8 BOM 与其它不可见控制字符；</li>
     *   <li>全角字符（含全角空格）转半角；</li>
     *   <li>英文大写转小写；</li>
     *   <li>统一换行符为 {@code '\n'}。</li>
     * </ol>
     * 标点符号会被保留，因为后续分句依赖它们。
     *
     * @param raw 原始文本，允许为 {@code null}
     * @return 规范化后的文本，不会返回 {@code null}
     */
    public static String normalize(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); ) {
            int codePoint = raw.codePointAt(i);
            i += Character.charCount(codePoint);
            if (codePoint == '\uFEFF') {
                continue;
            }
            if (codePoint == '\r') {
                // Windows 的 \r\n 只视为一次换行
                if (i < raw.length() && raw.charAt(i) == '\n') {
                    i++;
                }
                builder.append('\n');
                continue;
            }
            if (codePoint == '\t') {
                builder.append(' ');
                continue;
            }
            if (codePoint < 0x20 && codePoint != '\n') {
                continue;
            }
            if (codePoint == IDEOGRAPHIC_SPACE) {
                builder.append(' ');
                continue;
            }
            if (codePoint >= FULL_WIDTH_START && codePoint <= FULL_WIDTH_END) {
                codePoint -= FULL_WIDTH_OFFSET;
            }
            builder.appendCodePoint(Character.toLowerCase(codePoint));
        }
        return builder.toString();
    }

    /**
     * 提取「特征码点」：只保留汉字、字母与数字，丢弃空白、标点与各类符号。
     *
     * <p>使用码点而不是 {@code char}，可以正确处理增补平面上的汉字（如 emoji 之后的生僻字），
     * 不会把一个字符拆成两个无意义的代理项。</p>
     *
     * @param normalizedText 已经过 {@link #normalize(String)} 处理的文本
     * @return 特征码点数组，可能为空数组
     */
    public static int[] toFeatureCodePoints(String normalizedText) {
        if (normalizedText == null || normalizedText.isEmpty()) {
            return new int[0];
        }
        // 直接写入 int 数组，避免 List<Integer> 的装箱开销（大文件下这一项非常可观）
        int[] buffer = new int[normalizedText.length()];
        int size = 0;
        for (int i = 0; i < normalizedText.length(); ) {
            int codePoint = normalizedText.codePointAt(i);
            i += Character.charCount(codePoint);
            if (Character.isLetterOrDigit(codePoint)) {
                buffer[size++] = codePoint;
            }
        }
        return size == buffer.length ? buffer : Arrays.copyOf(buffer, size);
    }
}
