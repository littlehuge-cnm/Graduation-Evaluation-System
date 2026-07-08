@echo off
chcp 65001 >nul
title 毕业设计评价系统 - 启动脚本

set "ROOT_DIR=%~dp0"
set "BACKEND_DIR=%ROOT_DIR%"
set "FRONTEND_DIR=%ROOT_DIR%frontend"

:: 选择 Maven 命令
if exist "%BACKEND_DIR%mvnw.cmd" (
    set "MAVEN_CMD=%BACKEND_DIR%mvnw.cmd"
) else (
    where mvn >nul 2>nul
    if errorlevel 1 (
        echo [错误] 未找到 Maven。请先安装 Maven，或生成 mvnw.cmd（mvn wrapper:wrapper）。
        pause
        exit /b 1
    )
    set "MAVEN_CMD=mvn"
)

where npm >nul 2>nul
if errorlevel 1 (
    echo [错误] 未找到 npm。请先安装 Node.js。
    pause
    exit /b 1
)

echo [信息] 正在启动后端服务（Spring Boot）...
start "后端服务" /D "%BACKEND_DIR%" cmd /k "%MAVEN_CMD% spring-boot:run"

echo [信息] 等待后端服务启动...
timeout /t 20 /nobreak >nul

echo [信息] 正在启动前端服务（Vite）...
start "前端服务" /D "%FRONTEND_DIR%" cmd /k "npm run dev"

echo.
echo [成功] 前后端服务已启动。
echo 后端地址：http://localhost:8080
echo 前端地址：http://localhost:5173
echo Swagger 文档：http://localhost:8080/swagger-ui.html
echo.
echo 提示：关闭命令行窗口即可停止对应服务。
pause
