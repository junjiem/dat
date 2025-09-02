@echo off
:: Change the console code page to UTF-8(65001)
chcp 65001 > nul

set CURR_DIR=%~dp0
:: Current working directory where user executes the script
set USER_PWD=%cd%

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

:: Find JAR file
set JAR_FILE=
for %%f in (%CURR_DIR%\dat-cli-*.jar) do (
    set JAR_FILE=%%f
    goto :jar_found
)
echo ❌ Error: DAT CLI jar file not found in %CURR_DIR%
exit /b 1

:jar_found
:: Common part of Java execution command
set JAVA_CMD=java -Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8

:: Get first parameter as command
set COMMAND=%1

:: Check if -p or --project-path parameter is already specified
set HAS_PROJECT_PATH=false
setlocal enabledelayedexpansion
for %%a in (%*) do (
    if "%%a"=="-p" set HAS_PROJECT_PATH=true
    if "%%a"=="--project-path" set HAS_PROJECT_PATH=true
)

:: Check if command supports -p/--project-path parameter
set SUPPORTS_PROJECT_PATH=false
if not "%COMMAND%"=="" (
    :: server command always supports project-path parameter
    if "%COMMAND%"=="server" (
        set SUPPORTS_PROJECT_PATH=true
    ) else (
        :: Get command help info and check if it contains exactly "-p, --project-path=" pattern
        %JAVA_CMD% -jar "%JAR_FILE%" %COMMAND% --help 2>nul | findstr /R /C:"^[ ]*-p,[ ]*--project-path=" >nul 2>&1
        if not errorlevel 1 set SUPPORTS_PROJECT_PATH=true
    )
)

:: Extract project path from arguments for logging
set PROJECT_PATH=%USER_PWD%
set SKIP_NEXT=false
for %%a in (%*) do (
    if "!SKIP_NEXT!"=="true" (
        set PROJECT_PATH=%%~a
        set SKIP_NEXT=false
    ) else (
        if "%%a"=="-p" set SKIP_NEXT=true
        if "%%a"=="--project-path" set SKIP_NEXT=true
    )
)

:: Convert to absolute path if possible
if exist "%PROJECT_PATH%" (
    pushd "%PROJECT_PATH%" >nul 2>&1
    set PROJECT_PATH=!CD!
    popd >nul 2>&1
)

:: Build arguments list
set ARGS=%*
if "%HAS_PROJECT_PATH%"=="false" (
    if "%SUPPORTS_PROJECT_PATH%"=="true" (
        set ARGS=%* -p "%PROJECT_PATH%"
    )
)

:: Execute Java program with project path for logging
%JAVA_CMD% -Ddat.project.path="%PROJECT_PATH%" -jar "%JAR_FILE%" %ARGS%

:end
endlocal
