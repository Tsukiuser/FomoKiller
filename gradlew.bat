@if "%DEBUG%" == "" @echo off
if "%OS%"=="Windows_NT" setlocal

set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%

for %%i in ("%APP_HOME%") do set APP_HOME=%%~fi

set DEFAULT_JVM_OPTS="-Xmx64m" "-Xms64m"

if not defined JAVA_HOME (
    if exist "C:\Program Files\Android\Android Studio\jbr\bin\java.exe" (
        set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
    )
)

set GRADLE_BAT="C:\Users\David\.gradle\wrapper\dists\gradle-8.2-bin\bbg7u40eoinfdyxsxr3z4i7ta\gradle-8.2\bin\gradle.bat"
if exist %GRADLE_BAT% (
    call %GRADLE_BAT% %*
    exit /b %ERRORLEVEL%
)

exit /b 1
