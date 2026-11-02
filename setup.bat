@echo off
setlocal
pushd "%~dp0" || exit /b 1
set "SETUP_JAVA=java"
if defined JAVA_HOME set "SETUP_JAVA=%JAVA_HOME%\bin\java.exe"
"%SETUP_JAVA%" tools\Setup.java %*
set "SETUP_EXIT=%ERRORLEVEL%"
popd
exit /b %SETUP_EXIT%
