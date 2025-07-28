@echo off

set CURR_DIR=%~dp0

:: Set the JDK path you want to use
:: set JAVA_HOME=D:\development\Java\jdk-17.0.14

:: Update the PATH variable (only valid for the current script)
:: set PATH=%JAVA_HOME%\bin;%PATH%

:: Display the current java version and confirm whether the Settings have been successful
java -version

java -version >nul 2>&1
if errorlevel 1 (
    echo ❌ Error: Java was not found. Please install Java 17 or a higher version first
    exit /b 1
)

cd /d "%CURR_DIR%"

for %%f in (%CURR_DIR%\dat-cli-*.jar) do (
    java -Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 -jar "%%f" %*
    goto :end
)

:end

:: Wait for the user to press Enter before exiting
echo.
echo Please press Enter to exit
pause >nul
