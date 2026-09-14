@echo off
rem Build sources and package them into main.jar (no third-party dependency).
setlocal
cd /d "%~dp0"

rem Prefer JAVA_HOME, but fall back to PATH if it does not contain the toolchain.
set "JAVAC=javac"
set "JAR=jar"
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\javac.exe" set "JAVAC=%JAVA_HOME%\bin\javac.exe"
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\jar.exe" set "JAR=%JAVA_HOME%\bin\jar.exe"

if not exist build\classes mkdir build\classes

echo [1/2] Compiling sources ...
dir /s /b src\main\java\*.java > build\sources.txt
"%JAVAC%" -encoding UTF-8 -Xlint:all -d build\classes @build\sources.txt
if errorlevel 1 goto :error

echo [2/2] Packaging main.jar ...
"%JAR%" cfe main.jar com.zyy.papercheck.Main -C build\classes .
if errorlevel 1 goto :error

echo.
echo Build succeeded: main.jar
echo Try: java -jar main.jar samples\orig.txt samples\orig_add.txt samples\ans.txt
exit /b 0

:error
echo.
echo Build failed. Please check the compiler output above.
exit /b 1
