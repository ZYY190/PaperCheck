@echo off
rem 编译源码并打包成 main.jar（不依赖任何第三方库）
setlocal
cd /d "%~dp0"

if defined JAVA_HOME (set "JAVAC=%JAVA_HOME%\bin\javac.exe") else (set "JAVAC=javac")
if defined JAVA_HOME (set "JAR=%JAVA_HOME%\bin\jar.exe") else (set "JAR=jar")

if not exist build\classes mkdir build\classes

echo [1/2] 编译源码 ...
dir /s /b src\main\java\*.java > build\sources.txt
"%JAVAC%" -encoding UTF-8 -Xlint:all -d build\classes @build\sources.txt
if errorlevel 1 goto :error

echo [2/2] 打包 main.jar ...
"%JAR%" cfe main.jar com.zyy.papercheck.Main -C build\classes .
if errorlevel 1 goto :error

echo.
echo 构建成功：main.jar
echo 运行示例：java -jar main.jar samples\orig.txt samples\orig_add.txt samples\ans.txt
exit /b 0

:error
echo.
echo 构建失败，请检查上面的编译输出。
exit /b 1
