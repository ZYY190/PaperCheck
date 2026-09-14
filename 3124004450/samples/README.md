# 样例数据说明

课堂下发的样例（`orig.txt` 与若干 `orig_add.txt`、`orig_del.txt` 等）请放到本目录或任意目录，
程序只通过命令行参数读取路径，不依赖固定位置。

为方便自测，本目录提供了一组构造样例：

| 文件 | 与原文件的关系 | 说明 |
| --- | --- | --- |
| `orig.txt` | 原文 | 题目给出的示例原文 |
| `orig_add.txt` | 增 | 在原文基础上增加内容 |
| `orig_del.txt` | 删 | 在原文基础上删除内容 |
| `orig_dis.txt` | 改（调序） | 打乱分句顺序 |
| `orig_rep.txt` | 改（替换） | 替换大量字词 |
| `orig_other.txt` | 无关 | 主题完全不同的干扰项 |
| `thesis_orig.txt` | 原文 | 一段更接近真实论文的段落 |
| `thesis_copy.txt` | 抄袭版 | 对上一文件做了同义改写 |

运行方式：

```bat
java -jar ..\main.jar samples\orig.txt samples\orig_add.txt samples\answer.txt
```
