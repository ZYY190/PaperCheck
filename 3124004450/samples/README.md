# 样例数据说明

程序只通过命令行参数读取路径，不依赖文件的固定位置，因此样例放在哪里都可以。

## 一、课堂下发的测试文本（`official/`）

`official/` 目录是课堂下发的测试文本，全部为 UTF-8 编码：

| 文件 | 说明 |
| --- | --- |
| `orig.txt` | 原文（约 1.05 万字） |
| `orig_0.8_add.txt` | 在原文基础上增加内容 |
| `orig_0.8_del.txt` | 在原文基础上删除内容 |
| `orig_0.8_dis_1.txt` | 打乱程度较轻 |
| `orig_0.8_dis_10.txt` | 打乱程度中等 |
| `orig_0.8_dis_15.txt` | 打乱程度较重 |

实测结果（命令行运行 `main.jar`，Windows 10 + JDK 1.8.0_202）：

| 抄袭版文件 | 输出的重复率 | 耗时 |
| --- | ---: | ---: |
| `orig_0.8_add.txt` | 0.82 | 335 ms |
| `orig_0.8_del.txt` | 0.97 | 144 ms |
| `orig_0.8_dis_1.txt` | 0.97 | 177 ms |
| `orig_0.8_dis_10.txt` | 0.85 | 168 ms |
| `orig_0.8_dis_15.txt` | 0.71 | 150 ms |
| `orig.txt`（与自身比较，作对照） | 1.00 | 129 ms |

运行方式：

```bat
java -jar ..\main.jar samples\official\orig.txt samples\official\orig_0.8_del.txt samples\ans.txt
```

## 二、自建样例（本目录）

为了覆盖课堂样例没有覆盖到的情况（空文件、纯标点、单字句、主题完全无关等），
这里另外构造了一组小样例：

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
java -jar ..\main.jar samples\orig.txt samples\orig_add.txt samples\ans.txt
```
