@echo off
:: Change the console code page to UTF-8(65001)
chcp 65001 > nul

set CURR_DIR=%~dp0
:: Current working directory where user executes the script
set USER_PWD=%cd%

:: Try to use bundled JRE first, fall back to system Java if not found
set BUNDLED_JRE=%CURR_DIR%..\jre
if exist "%BUNDLED_JRE%\bin\java.exe" (
    :: Use bundled JRE
    set JAVA_CMD="%BUNDLED_JRE%\bin\java.exe"
    echo Using bundled JRE from %BUNDLED_JRE%
) else (
    :: Check if system Java is available
    java -version >nul 2>&1
    if errorlevel 1 (
        echo ❌ Error: Java was not found. Please install Java 17 or a higher version first
        exit /b 1
    )
    :: Use system Java
    set JAVA_CMD=java
    echo Using system Java
)

:: Find JAR file
set JAR_FILE=
for %%f in (%CURR_DIR%\..\dat-cli-*.jar) do (
    set JAR_FILE=%%f
    goto :jar_found
)
echo ❌ Error: DAT CLI jar file not found in %CURR_DIR%\..
exit /b 1
:jar_found

:: Common Java options
set JAVA_OPTS=-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 -Xms256m -Xmx2g

:: Get first parameter as command
set COMMAND=%1

:: Check if -p or --project-path parameter is already specified
set HAS_PROJECT_PATH=false
:: Check if -w or --workspace-path parameter is already specified
set HAS_WORKSPACE_PATH=false
setlocal enabledelayedexpansion
for %%a in (%*) do (
    if "%%a"=="-p" set HAS_PROJECT_PATH=true
    if "%%a"=="--project-path" set HAS_PROJECT_PATH=true
    if "%%a"=="-w" set HAS_WORKSPACE_PATH=true
    if "%%a"=="--workspace-path" set HAS_WORKSPACE_PATH=true
)

:: Check if command supports -p/--project-path parameter
set SUPPORTS_PROJECT_PATH=false
if not "%COMMAND%"=="" (
    :: Check if command is one of the supported commands
    if "%COMMAND%"=="build" set SUPPORTS_PROJECT_PATH=true
    if "%COMMAND%"=="clean" set SUPPORTS_PROJECT_PATH=true
    if "%COMMAND%"=="list" set SUPPORTS_PROJECT_PATH=true
    if "%COMMAND%"=="run" set SUPPORTS_PROJECT_PATH=true
    if "%COMMAND%"=="server" set SUPPORTS_PROJECT_PATH=true
    if "%COMMAND%"=="seed" set SUPPORTS_PROJECT_PATH=true
)

:: Check if command supports -w/--workspace-path parameter
set SUPPORTS_WORKSPACE_PATH=false
if not "%COMMAND%"=="" (
    :: Check if command is one of the supported commands
    if "%COMMAND%"=="init" set SUPPORTS_WORKSPACE_PATH=true
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

:: Convert to absolute path
for %%i in ("%PROJECT_PATH%") do set PROJECT_PATH=%%~fi

:: Extract workspace path from arguments for logging
set WORKSPACE_PATH=%USER_PWD%
set SKIP_NEXT_W=false
for %%a in (%*) do (
    if "!SKIP_NEXT_W!"=="true" (
        set WORKSPACE_PATH=%%~a
        set SKIP_NEXT_W=false
    ) else (
        if "%%a"=="-w" set SKIP_NEXT_W=true
        if "%%a"=="--workspace-path" set SKIP_NEXT_W=true
    )
)

:: Convert to absolute path
for %%i in ("%WORKSPACE_PATH%") do set WORKSPACE_PATH=%%~fi

:: Build arguments list
set ARGS=%*
if "%HAS_PROJECT_PATH%"=="false" (
    if "%SUPPORTS_PROJECT_PATH%"=="true" (
        set ARGS=!ARGS! -p "%PROJECT_PATH%"
    )
)

:: Check if need to add workspace path parameter
if "%HAS_WORKSPACE_PATH%"=="false" (
    if "%SUPPORTS_WORKSPACE_PATH%"=="true" (
        set ARGS=!ARGS! -w "%WORKSPACE_PATH%"
    )
)

:: Set logs root path based on whether command supports project path
if "%SUPPORTS_PROJECT_PATH%"=="true" (
    set LOGS_ROOT_PATH=%PROJECT_PATH%
) else (
    set LOGS_ROOT_PATH=%CURR_DIR%\..
)

:: Execute Java program with logs root path for logging
%JAVA_CMD% %JAVA_OPTS% -Ddat.logs.root.path="%LOGS_ROOT_PATH%" -jar "%JAR_FILE%" %ARGS%

:end
endlocal
