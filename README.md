# PaperCheck —— 论文查重（软件工程个人项目）

本仓库是《软件工程》课程个人作业的提交仓库。作业内容为设计并实现一个**论文查重算法**：
给定一份原文和一份在其基础上经过增、删、改的抄袭版论文，计算并输出两者的重复率。

## 仓库地址

- 仓库主页：<https://github.com/ZYY190/PaperCheck>
- 作业目录（学号）：<https://github.com/ZYY190/PaperCheck/tree/main/3124004450>
- 可执行程序 main.jar：见本仓库的 [Releases](https://github.com/ZYY190/PaperCheck/releases) 页面

## 目录结构

```text
PaperCheck/
└── 3124004450/                 # 学号目录，作业的全部内容都在这里
    ├── src/main/java/...       # 程序源码
    ├── src/test/java/...       # 单元测试
    ├── samples/                # 自测样例数据
    ├── tools/java/             # 性能基准与采样负载（非程序主体，IDEA 中已标记为源码根）
    ├── docs/                   # 覆盖率报告、测试报告、性能分析
    ├── lib/                    # 单元测试所需的第三方 jar
    ├── main.jar                # 编译好的可执行程序
    ├── PSP.md                  # PSP 表格（预估 / 实际）
    ├── build.bat               # 一键编译打包
    ├── run-tests.bat           # 一键跑单元测试与覆盖率
    ├── run-benchmark.bat       # 一键跑性能基准
    └── run-profile.bat         # 一键启动可采样的分析负载
```

## 运行方式

程序要求三个命令行参数，均为绝对路径，且路径中不能包含空格：

```bat
java -jar main.jar <原文文件> <抄袭版论文文件> <答案文件>
```

例如：

```bat
java -jar main.jar C:\tests\orig.txt C:\tests\orig_add.txt C:\tests\ans.txt
```

答案文件中的内容是一个保留两位小数的浮点数，例如 `0.88`。

## 环境要求

- JDK 8 及以上（开发与验证使用 JDK 1.8.0_202，64 位 Windows 10）
- 程序主体不依赖任何第三方库，`main.jar` 开箱即用
- 运行单元测试需要 `lib/` 下的 JUnit 5 与 JaCoCo（已随仓库提供，无需联网）

## 文档索引

| 文档 | 内容 |
| --- | --- |
| [3124004450/README.md](3124004450/README.md) | 需求分析、算法设计、模块划分、异常设计与测试说明 |
| [3124004450/PSP.md](3124004450/PSP.md) | PSP 2.1 表格（预估耗时与修正后的实际耗时） |
| [3124004450/docs/performance.md](3124004450/docs/performance.md) | 性能基准与 CPU 热点分析 |
| [3124004450/docs/coverage/index.html](3124004450/docs/coverage/index.html) | JaCoCo 覆盖率报告 |
| [3124004450/docs/test-report.txt](3124004450/docs/test-report.txt) | 单元测试完整输出 |
