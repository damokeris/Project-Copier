@echo off
setlocal enabledelayedexpansion
:: 使用UTF-8代码页，以防项目名或路径包含非英文字符
chcp 65001 >nul
title Java Source Code Copier

:: ========================= 配置区域 =========================
:: 1. 你的IdeaProjects目录的根路径
set "PROJECTS_ROOT=%USERPROFILE%\IdeaProjects"

:: 2. 拷贝代码的目标根目录
set "DOC_ROOT=%USERPROFILE%\Documents\CODE"
:: ==========================================================


:: --- 1. 查找并显示可用的项目 ---
cls
echo.
echo =============================================================
echo      Java Source Code Copier (Interactive Mode)
echo =============================================================
echo.

if not exist "%PROJECTS_ROOT%" (
    echo [错误] 未找到IdeaProjects目录: %PROJECTS_ROOT%
    echo.
    echo 请检查脚本中的 "PROJECTS_ROOT" 配置是否正确。
    pause
    goto :eof
)

echo [信息] 正在扫描 "%PROJECTS_ROOT%" 下的项目...
echo.
set "project_count=0"
for /d %%P in ("%PROJECTS_ROOT%\*") do (
    set /a project_count+=1
    set "project[!project_count!]=%%~nxP"
    echo   !project_count!. %%~nxP
)

if %project_count% equ 0 (
    echo [警告] 在 "%PROJECTS_ROOT%" 目录中没有找到任何项目文件夹。
    pause
    goto :eof
)
echo.
echo -------------------------------------------------------------


:: --- 2. 获取并验证用户输入 ---
:get_choice
echo.
set "choice="
set /p "choice=请输入你想要拷贝的项目的编号: "

:: ==== vvvvvvvvvvvvvvvvvvvvvv 修改后的验证逻辑 vvvvvvvvvvvvvvvvvvvvvv ====
if not defined choice goto :invalid_choice

:: 使用 set /a 验证输入是否为纯数字
set /a "check_num=choice" >nul 2>nul

:: 如果输入 "abc", check_num 会变成 0. 如果输入 "5a", check_num 会变成 5.
:: 所以，我们比较转换后的数字和原始输入字符串是否一致。
:: 并且，我们确保数字大于0。
if "%check_num%"=="" goto :invalid_choice
if "%check_num%"=="0" goto :invalid_choice
if not "%check_num%"=="%choice%" goto :invalid_choice

:: 验证数字是否在有效范围内
if %check_num% gtr %project_count% goto :invalid_choice

:: 如果所有验证都通过，将 check_num 赋值给 choice
set "choice=%check_num%"
goto :valid_choice
:: ==== ^^^^^^^^^^^^^^^^^^^^^^ 修改后的验证逻辑 ^^^^^^^^^^^^^^^^^^^^^^ ====

:invalid_choice
echo.
echo [错误] 输入无效，请输入列表中一个有效的数字。
goto get_choice


:: --- 3. 确定源路径和目标路径 ---
:valid_choice
set "PROJECT_NAME=!project[%choice%]!"

:: 智能探测源代码路径：优先使用 src/main/java，否则使用项目根目录
set "SOURCE_PATH=%PROJECTS_ROOT%\!PROJECT_NAME!\src\main\java"
if not exist "!SOURCE_PATH!\" (
    echo [信息] 未找到 "!PROJECT_NAME!\src\main\java"，将从项目根目录拷贝。
    set "SOURCE_PATH=%PROJECTS_ROOT%\!PROJECT_NAME!"
)

:: 构建完整的目标路径
set "DEST=%DOC_ROOT%\%PROJECT_NAME%"


:: --- 4. 执行拷贝操作 ---
:: ... 后续代码与之前完全相同，无需修改 ...
cls
echo.
echo [信息] 正在扁平化复制Java项目文件...
echo   项目名称: %PROJECT_NAME%
echo   源目录:   %SOURCE_PATH%
echo   目标目录: %DEST%
echo.

if not exist "%DEST%" (
    echo [操作] 正在创建目标目录: %DEST%
    mkdir "%DEST%" >nul 2>&1
)

set file_count=0
set conflict_count=0

echo [操作] 正在收集并仅复制 .java 文件...
for /R "%SOURCE_PATH%" %%F in (*.java) do (
    set /a file_count += 1
    set "filename=%%~nxF"
    set "dest_file=%DEST%\!filename!"
    
    if exist "!dest_file!" (
        set /a conflict_count += 1
        set "counter=1"
        :retry
        set "new_name=%%~nF_!counter!%%~xF"
        set "dest_file=%DEST%\!new_name!"
        if exist "!dest_file!" (
            set /a counter += 1
            goto :retry
        )
        copy "%%F" "!dest_file!" >nul
        echo   [重命名] %%~nxF -> !new_name!
    ) else (
        copy "%%F" "!dest_file!" >nul
        echo   [复制] %%~nxF
    )
)


:: --- 5. 显示结果摘要 ---
echo.
echo ====================== 操 作 完 成 ======================
if %file_count% equ 0 (
    echo   在指定源目录中未找到任何 .java 文件。
) else (
    echo   处理 .java 文件总数: %file_count%
    echo   因重名而重命名文件数: %conflict_count%
    echo   所有文件已复制到:
    echo   %DEST%
)
echo =========================================================
echo.

pause
endlocal
