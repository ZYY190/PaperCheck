package com.zyy.papercheck;

/**
 * 项目自定义的检查异常。
 *
 * <p>程序中所有「可以预期」的错误（参数个数不对、文件不存在、编码无法识别、文件超过容量上限等）
 * 都通过该异常向上抛出，最终由 {@link Main} 统一翻译成用户可读的错误提示和进程退出码，
 * 避免把 Java 堆栈直接暴露给评测程序。</p>
 *
 * @author zyy
 */
public class PaperCheckException extends Exception {

    private static final long serialVersionUID = 1L;

    /** 命令行参数个数不正确。 */
    public static final String CODE_ARGUMENT_COUNT = "E001";
    /** 输入文件不存在。 */
    public static final String CODE_FILE_NOT_FOUND = "E002";
    /** 路径指向的不是普通文件。 */
    public static final String CODE_NOT_A_FILE = "E003";
    /** 文件读写失败（权限不足、被其它进程占用等）。 */
    public static final String CODE_IO_ERROR = "E004";
    /** 文件编码无法识别。 */
    public static final String CODE_ENCODING = "E005";
    /** 文件体积超过程序允许的上限。 */
    public static final String CODE_FILE_TOO_LARGE = "E006";
    /** 答案文件路径非法或不可写。 */
    public static final String CODE_OUTPUT_INVALID = "E007";

    /** 错误码，便于单元测试精确断言。 */
    private final String code;

    /**
     * @param code    错误码，取值见本类中的 CODE_* 常量
     * @param message 面向使用者的错误描述
     */
    public PaperCheckException(String code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * @param code    错误码
     * @param message 面向使用者的错误描述
     * @param cause   底层原因，保留堆栈便于排查
     */
    public PaperCheckException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    /**
     * @return 错误码
     */
    public String getCode() {
        return code;
    }
}
