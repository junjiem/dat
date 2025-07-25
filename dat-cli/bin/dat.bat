@echo off

set CURR_DIR=%~dp0

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
