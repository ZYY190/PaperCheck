@echo off
rem 性能基准测试：对比朴素整篇比对与分句+索引剪枝两种实现
setlocal
cd /d "%~dp0"

if defined JAVA_HOME (set "JAVAC=%JAVA_HOME%\bin\javac.exe") else (set "JAVAC=javac")
if defined JAVA_HOME (set "JAVA=%JAVA_HOME%\bin\java.exe") else (set "JAVA=java")

if not exist build\classes mkdir build\classes
dir /s /b src\main\java\*.java > build\sources.txt
"%JAVAC%" -encoding UTF-8 -d build\classes @build\sources.txt
if errorlevel 1 exit /b 1

"%JAVAC%" -encoding UTF-8 -cp build\classes -d build\classes tools\Benchmark.java
if errorlevel 1 exit /b 1

"%JAVA%" -Dfile.encoding=UTF-8 -cp build\classes com.zyy.papercheck.Benchmark
endlocal
