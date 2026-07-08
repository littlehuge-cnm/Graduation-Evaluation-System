@ECHO OFF
SETLOCAL

set "DIRNAME=%~dp0"
if "%DIRNAME%"=="" set DIRNAME=.

powershell -NoProfile -ExecutionPolicy Bypass -File "%DIRNAME%.mvn\wrapper\wrapper.ps1" %*

if %ERRORLEVEL% neq 0 exit /b %ERRORLEVEL%
