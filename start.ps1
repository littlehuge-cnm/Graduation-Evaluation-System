# 启动脚本：同时启动前后端服务
# 使用方法：右键选择"使用 PowerShell 运行"，或在终端执行 .\start.ps1

$RootDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
$BackendDir = $RootDir
$FrontendDir = Join-Path $RootDir "frontend"

function Write-Info { param([string]$msg) Write-Host $msg -ForegroundColor Cyan }
function Write-Success { param([string]$msg) Write-Host $msg -ForegroundColor Green }
function Write-Warn { param([string]$msg) Write-Host $msg -ForegroundColor Yellow }
function Write-ErrorMsg { param([string]$msg) Write-Host $msg -ForegroundColor Red }

# 递归终止进程树
function Stop-ProcessTree {
    param([int]$Id)
    Get-CimInstance Win32_Process | Where-Object { $_.ParentProcessId -eq $Id } | ForEach-Object {
        Stop-ProcessTree -Id $_.ProcessId
    }
    Stop-Process -Id $Id -Force -ErrorAction SilentlyContinue
}

# 选择 Maven 命令
$MavenCmd = $null
if (Test-Path (Join-Path $BackendDir "mvnw.cmd")) {
    $MavenCmd = Join-Path $BackendDir "mvnw.cmd"
} elseif (Get-Command mvn -ErrorAction SilentlyContinue) {
    $MavenCmd = "mvn"
} else {
    Write-ErrorMsg "错误：未找到 Maven。请先安装 Maven，或生成 mvnw.cmd（mvn wrapper:wrapper）。"
    exit 1
}

if (-not (Get-Command npm -ErrorAction SilentlyContinue)) {
    Write-ErrorMsg "错误：未找到 npm。请先安装 Node.js。"
    exit 1
}

Write-Info "项目根目录：$RootDir"
Write-Info "Maven 命令：$MavenCmd"

# 启动后端
Write-Info "正在启动后端服务（Spring Boot）..."
$BackendProcess = Start-Process -FilePath $MavenCmd -ArgumentList "spring-boot:run" -WorkingDirectory $BackendDir -WindowStyle Normal -PassThru

# 等待后端端口 8080 就绪
Write-Info "等待后端服务就绪（端口 8080）..."
$MaxAttempts = 60
$Ready = $false
for ($i = 0; $i -lt $MaxAttempts; $i++) {
    Start-Sleep -Seconds 2
    $Connection = Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue
    if ($Connection) {
        $Ready = $true
        break
    }
}

if (-not $Ready) {
    Write-ErrorMsg "后端服务在 120 秒内未就绪，请检查后端窗口中的错误日志。"
    exit 1
}
Write-Success "后端服务已就绪：http://localhost:8080"

# 启动前端
Write-Info "正在启动前端服务（Vite）..."
$FrontendProcess = Start-Process -FilePath "npm" -ArgumentList "run", "dev" -WorkingDirectory $FrontendDir -WindowStyle Normal -PassThru

Write-Success "前后端服务已启动。"
Write-Host ""
Write-Host "后端地址：http://localhost:8080"
Write-Host "前端地址：http://localhost:5173"
Write-Host "Swagger 文档：http://localhost:8080/swagger-ui.html"
Write-Host ""
Write-Host "按任意键关闭所有服务..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")

# 关闭服务
Write-Info "正在关闭服务..."
Stop-ProcessTree -Id $BackendProcess.Id
Stop-ProcessTree -Id $FrontendProcess.Id
Write-Success "服务已关闭。"
