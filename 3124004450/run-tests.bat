@echo off
rem Run all unit tests and generate the JaCoCo coverage report.
setlocal
cd /d "%~dp0"

rem Prefer JAVA_HOME, but fall back to PATH if it does not contain the toolchain.
set "JAVAC=javac"
set "JAVA=java"
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\javac.exe" set "JAVAC=%JAVA_HOME%\bin\javac.exe"
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "JAVA=%JAVA_HOME%\bin\java.exe"

set "JUNIT=lib\junit-platform-console-standalone-1.11.4.jar"
set "JACOCO_AGENT=lib\org.jacoco.agent-0.8.12-runtime.jar"
set "JACOCO_CLI=lib\org.jacoco.cli-0.8.12-nodeps.jar"

if not exist "%JUNIT%" (
    echo Cannot find %JUNIT%. Please make sure the lib folder is complete.
    exit /b 1
)

if not exist build\classes mkdir build\classes
if not exist build\test-classes mkdir build\test-classes

echo [1/4] Compiling main sources ...
dir /s /b src\main\java\*.java > build\sources.txt
"%JAVAC%" -encoding UTF-8 -Xlint:all -d build\classes @build\sources.txt
if errorlevel 1 exit /b 1

echo [2/4] Compiling test sources ...
dir /s /b src\test\java\*.java > build\test-sources.txt
"%JAVAC%" -encoding UTF-8 -cp "build\classes;%JUNIT%" -d build\test-classes @build\test-sources.txt
if errorlevel 1 exit /b 1

echo [3/4] Running unit tests with JaCoCo agent ...
"%JAVA%" -Dfile.encoding=UTF-8 -javaagent:"%JACOCO_AGENT%=destfile=build\jacoco.exec,includes=com.zyy.papercheck.*" ^
    -jar "%JUNIT%" execute --class-path "build\classes;build\test-classes" --scan-class-path --disable-ansi-colors
if errorlevel 1 exit /b 1

echo [4/4] Generating coverage report ...
if not exist build\coverage mkdir build\coverage
"%JAVA%" -jar "%JACOCO_CLI%" report build\jacoco.exec --classfiles build\classes ^
    --sourcefiles src\main\java --html build\coverage --csv build\coverage\coverage.csv --name PaperCheck
if errorlevel 1 exit /b 1

echo.
echo Coverage report: build\coverage\index.html
endlocal
