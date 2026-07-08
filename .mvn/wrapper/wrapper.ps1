# Maven Wrapper 简化脚本（only-script 模式）
# 根据 .mvn/wrapper/maven-wrapper.properties 自动下载并运行 Maven

$ErrorActionPreference = "Stop"

$propertiesPath = Join-Path $PSScriptRoot "maven-wrapper.properties"
if (-not (Test-Path $propertiesPath)) {
    Write-Error "未找到配置文件：$propertiesPath"
    exit 1
}

$properties = @{}
Get-Content $propertiesPath | ForEach-Object {
    if ($_ -match "^\s*([^#\s][^=]*?)\s*=\s*(.+?)\s*$") {
        $properties[$matches[1]] = $matches[2]
    }
}

$distributionUrl = $properties["distributionUrl"]
if (-not $distributionUrl) {
    Write-Error "maven-wrapper.properties 中未找到 distributionUrl"
    exit 1
}

# 解析文件名，例如 apache-maven-3.9.16-bin.zip
$uri = [System.Uri]$distributionUrl
$fileName = [System.IO.Path]::GetFileName($uri.LocalPath)
if ($fileName -notmatch "^(apache-maven-(.+))-bin\.zip$") {
    Write-Error "无法解析分发包文件名：$fileName"
    exit 1
}
$mavenDirName = $matches[1]
$mavenVersion = $matches[2]

# 计算基于 URL 的缓存子目录
$sha256 = [System.Security.Cryptography.SHA256]::Create()
$urlBytes = [System.Text.Encoding]::UTF8.GetBytes($distributionUrl)
$hashBytes = $sha256.ComputeHash($urlBytes)
$hash = [BitConverter]::ToString($hashBytes).Replace("-", "").ToLower()

$wrapperHome = [System.IO.Path]::Combine($env:USERPROFILE, ".m2", "wrapper", "dists")
$installDir = [System.IO.Path]::Combine($wrapperHome, $mavenDirName, $hash)
$mavenHome = [System.IO.Path]::Combine($installDir, $mavenDirName)
$mvnCmd = [System.IO.Path]::Combine($mavenHome, "bin", "mvn.cmd")

# 下载并解压 Maven
if (-not (Test-Path $mvnCmd)) {
    if (-not (Test-Path $installDir)) {
        New-Item -ItemType Directory -Force -Path $installDir | Out-Null
    }
    $zipPath = Join-Path $installDir "$mavenDirName-bin.zip"

    Write-Host "[Maven Wrapper] 正在下载 Maven $mavenVersion ..." -ForegroundColor Cyan
    Write-Host "[Maven Wrapper] $distributionUrl"
    try {
        Invoke-WebRequest -Uri $distributionUrl -OutFile $zipPath -UseBasicParsing
    } catch {
        Write-Error "下载 Maven 失败：$_"
        exit 1
    }

    Write-Host "[Maven Wrapper] 正在解压 Maven ..." -ForegroundColor Cyan
    Expand-Archive -Path $zipPath -DestinationPath $installDir -Force
    Remove-Item $zipPath -Force
}

# 执行 Maven
& $mvnCmd @args
exit $LASTEXITCODE
