@echo off
where gradle >nul 2>nul
if %ERRORLEVEL% EQU 0 (gradle %*) else (echo Gradle 9.5+ is required. Use Android Studio or install Gradle 9.5+.& exit /b 1)
