@echo off
rem Benchmark: compare the naive whole-text comparison with the indexed implementation.
setlocal
cd /d "%~dp0"

rem Prefer JAVA_HOME, but fall back to PATH if it does not contain the toolchain.
set "JAVAC=javac"
set "JAVA=java"
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\javac.exe" set "JAVAC=%JAVA_HOME%\bin\javac.exe"
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "JAVA=%JAVA_HOME%\bin\java.exe"

if not exist build\classes mkdir build\classes
dir /s /b src\main\java\*.java > build\sources.txt
"%JAVAC%" -encoding UTF-8 -d build\classes @build\sources.txt
if errorlevel 1 exit /b 1

"%JAVAC%" -encoding UTF-8 -cp build\classes -d build\classes tools\java\com\zyy\papercheck\Benchmark.java
if errorlevel 1 exit /b 1

"%JAVA%" -Dfile.encoding=UTF-8 -cp build\classes com.zyy.papercheck.Benchmark
endlocal
