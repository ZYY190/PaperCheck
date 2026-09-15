@echo off
rem Run a long enough workload for CPU profiling (use it together with a profiler).
rem Usage: run-profile.bat [chars] [rounds]
setlocal
cd /d "%~dp0"

set "SIZE=%~1"
set "ROUNDS=%~2"
if "%SIZE%"=="" set "SIZE=500000"
if "%ROUNDS%"=="" set "ROUNDS=30"

rem Prefer JAVA_HOME, but fall back to PATH if it does not contain the toolchain.
set "JAVAC=javac"
set "JAVA=java"
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\javac.exe" set "JAVAC=%JAVA_HOME%\bin\javac.exe"
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "JAVA=%JAVA_HOME%\bin\java.exe"

if not exist build\classes mkdir build\classes
dir /s /b src\main\java\*.java > build\sources.txt
"%JAVAC%" -encoding UTF-8 -d build\classes @build\sources.txt
if errorlevel 1 exit /b 1

"%JAVAC%" -encoding UTF-8 -cp build\classes -d build\classes tools\java\com\zyy\papercheck\Benchmark.java tools\java\com\zyy\papercheck\ProfileTarget.java
if errorlevel 1 exit /b 1

echo.
echo Profiling workload: %SIZE% chars x %ROUNDS% rounds
echo Attach your profiler (VisualVM / IDEA Profiler) to this JVM, then wait for it to finish.
echo.
"%JAVA%" -Dfile.encoding=UTF-8 -cp build\classes com.zyy.papercheck.ProfileTarget %SIZE% %ROUNDS%
endlocal
