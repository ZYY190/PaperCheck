@echo off
rem 运行全部单元测试，并生成 JaCoCo 覆盖率报告
setlocal
cd /d "%~dp0"

if defined JAVA_HOME (set "JAVAC=%JAVA_HOME%\bin\javac.exe") else (set "JAVAC=javac")
if defined JAVA_HOME (set "JAVA=%JAVA_HOME%\bin\java.exe") else (set "JAVA=java")

set "JUNIT=lib\junit-platform-console-standalone-1.11.4.jar"
set "JACOCO_AGENT=lib\org.jacoco.agent-0.8.12-runtime.jar"
set "JACOCO_CLI=lib\org.jacoco.cli-0.8.12-nodeps.jar"

if not exist "%JUNIT%" (
    echo 找不到 %JUNIT%，请确认 lib 目录完整。
    exit /b 1
)

if not exist build\classes mkdir build\classes
if not exist build\test-classes mkdir build\test-classes

echo [1/4] 编译主程序 ...
dir /s /b src\main\java\*.java > build\sources.txt
"%JAVAC%" -encoding UTF-8 -Xlint:all -d build\classes @build\sources.txt
if errorlevel 1 exit /b 1

echo [2/4] 编译测试代码 ...
dir /s /b src\test\java\*.java > build\test-sources.txt
"%JAVAC%" -encoding UTF-8 -cp "build\classes;%JUNIT%" -d build\test-classes @build\test-sources.txt
if errorlevel 1 exit /b 1

echo [3/4] 运行单元测试（JaCoCo 采集覆盖率）...
"%JAVA%" -Dfile.encoding=UTF-8 -javaagent:"%JACOCO_AGENT%=destfile=build\jacoco.exec,includes=com.zyy.papercheck.*" ^
    -jar "%JUNIT%" execute --class-path "build\classes;build\test-classes" --scan-class-path --disable-ansi-colors
if errorlevel 1 exit /b 1

echo [4/4] 生成覆盖率报告 ...
if not exist build\coverage mkdir build\coverage
"%JAVA%" -jar "%JACOCO_CLI%" report build\jacoco.exec --classfiles build\classes ^
    --sourcefiles src\main\java --html build\coverage --csv build\coverage\coverage.csv --name PaperCheck
if errorlevel 1 exit /b 1

echo.
echo 覆盖率报告：build\coverage\index.html
endlocal
