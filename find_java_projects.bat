@echo off
setlocal DISABLEDELAYEDEXPANSION  ; 关键修改：禁用延迟扩展
chcp 65001 >nul
title Java-Code-Copier

:: ===================== CONFIGURATION =====================
set "PROJECTS_ROOT=%USERPROFILE%\VSCodeProjects"
set "DOC_ROOT=%USERPROFILE%\Documents\CODE"
:: ========================================================

:: --- 1. Scan projects ---
cls
echo.
echo ======================================================
echo        Java Source Code Copier (Interactive)
echo ======================================================
echo.

if not exist "%PROJECTS_ROOT%" (
    echo [ERROR] Directory not found: %PROJECTS_ROOT%
    echo.
    pause
    goto :eof
)

echo [INFO] Scanning "%PROJECTS_ROOT%"...
echo.
set project_count=0
for /d %%P in ("%PROJECTS_ROOT%\*") do (
    set /a project_count+=1
    call set "project_%%project_count%%=%%~nxP"
    echo   !project_count!. %%~nxP
)

if %project_count% equ 0 (
    echo [WARN] No projects found in "%PROJECTS_ROOT%"
    pause
    goto :eof
)
echo ------------------------------------------------------

:: --- 2. User input ---
:get_choice
echo.
set /p "choice=Enter project number: "

if "%choice%"=="" goto invalid
echo %choice%|findstr /r "^[0-9][0-9]*$" >nul || goto invalid
if %choice% LSS 1 goto invalid
if %choice% GTR %project_count% goto invalid
goto valid_choice

:invalid
echo [ERROR] Invalid input. Please enter a number between 1 and %project_count%
goto get_choice

:: --- 3. Path setup ---
:valid_choice
call set "PROJECT_NAME=%%project_%choice%%%"

set "SOURCE_PATH=%PROJECTS_ROOT%\%PROJECT_NAME%\src\main\java"
if not exist "%SOURCE_PATH%\" (
    echo [INFO] Using project root: %PROJECT_NAME%
    set "SOURCE_PATH=%PROJECTS_ROOT%\%PROJECT_NAME%"
)

set "DEST=%DOC_ROOT%\%PROJECT_NAME%"

:: --- 4. Copy operation (FIXED) ---
cls
echo.
echo [INFO] Copying Java files...
echo   Project: %PROJECT_NAME%
echo   Source:  %SOURCE_PATH%
echo   Target:  %DEST%
echo.

if not exist "%DEST%" mkdir "%DEST%" >nul 2>&1

set file_count=0
set conflict_count=0

for /R "%SOURCE_PATH%" %%F in (*.java) do (
    set /a file_count += 1
    set "source_file=%%F"
    set "dest_file=%DEST%\%%~nxF"
    
    setlocal ENABLEDELAYEDEXPANSION
    if exist "!dest_file!" (
        set /a conflict_count += 1
        set counter=1
        call :resolve_conflict "!source_file!" "!dest_file!"
    ) else (
        copy "!source_file!" "!dest_file!" >nul
        echo   [COPY] %%~nxF
    )
    endlocal
)

:: --- 5. Results ---
echo.
echo ====================== SUMMARY =======================
if %file_count% equ 0 (
    echo   No .java files found
) else (
    echo   Total files:    %file_count%
    echo   Renamed files:  %conflict_count%
    echo   Output folder:  %DEST%
)
echo =====================================================
echo.
pause
exit /b

:: --- Subroutine for conflict resolution ---
:resolve_conflict
set "src=%~1"
set "dst=%~2"
set counter=1

:retry
set "new_name=%~n1_%counter%%~x1"
set "new_dest=%DEST%\%new_name%"

if exist "%new_dest%" (
    set /a counter += 1
    goto :retry
)

copy "%src%" "%new_dest%" >nul
echo   [RENAME] %~nx1 --^> %new_name%
exit /b
