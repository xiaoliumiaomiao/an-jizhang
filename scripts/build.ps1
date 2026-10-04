[CmdletBinding()]
param(
    [string[]]$Tasks = @(':app:testDebugUnitTest', ':app:lintDebug', ':app:assembleDebug'),
    [string]$JavaHomePath,
    [string]$SdkRootPath
)

$ErrorActionPreference = 'Stop'
$projectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$configPath = Join-Path $projectRoot '.tooling\toolchain.json'
if (Test-Path -LiteralPath $configPath) {
    $toolchain = Get-Content -LiteralPath $configPath -Raw | ConvertFrom-Json
    if (-not $JavaHomePath) { $JavaHomePath = $toolchain.javaHome }
    if (-not $SdkRootPath) { $SdkRootPath = $toolchain.sdkRoot }
}
if (-not $JavaHomePath) { $JavaHomePath = $env:JAVA_HOME }
if (-not $SdkRootPath) { $SdkRootPath = $env:ANDROID_HOME }
if (-not $JavaHomePath -or -not (Test-Path -LiteralPath (Join-Path $JavaHomePath 'bin\java.exe'))) {
    throw '未找到 JDK。请先按 docs/DEVELOPMENT.md 准备工具，或传入 -JavaHomePath。'
}
if (-not $SdkRootPath -or -not (Test-Path -LiteralPath (Join-Path $SdkRootPath 'platforms\android-35\android.jar'))) {
    throw '未找到 Android 35 SDK。请先按 docs/DEVELOPMENT.md 准备工具，或传入 -SdkRootPath。'
}
$previousJava = $env:JAVA_HOME
$previousAndroid = $env:ANDROID_HOME
$previousSdk = $env:ANDROID_SDK_ROOT
$previousGradle = $env:GRADLE_USER_HOME
$logDirectory = Join-Path $projectRoot 'test-artifacts'
New-Item -ItemType Directory -Path $logDirectory -Force | Out-Null
$logPath = Join-Path $logDirectory ('build-{0}.log' -f (Get-Date -Format 'yyyyMMdd-HHmmss'))

try {
    # 仅修改当前进程，finally 恢复；不写系统 PATH 或用户环境变量。
    $env:JAVA_HOME = $JavaHomePath
    $env:ANDROID_HOME = $SdkRootPath
    $env:ANDROID_SDK_ROOT = $SdkRootPath
    $env:GRADLE_USER_HOME = Join-Path $projectRoot '.tooling\gradle-cache'
    Push-Location -LiteralPath $projectRoot
    try {
        $previousErrorPolicy = $ErrorActionPreference
        try {
            # Windows PowerShell 5.1 将 native stderr 包装为错误记录；
            # 先收集完整输出，以进程退出码判断 Gradle 成败。
            $ErrorActionPreference = 'Continue'
            & (Join-Path $projectRoot 'gradlew.bat') @Tasks --no-daemon 2>&1 |
                ForEach-Object { $_.ToString() } | Tee-Object -FilePath $logPath -ErrorAction Stop
            $result = $LASTEXITCODE
        } finally {
            $ErrorActionPreference = $previousErrorPolicy
        }
    } finally {
        Pop-Location
    }
    if ($result -ne 0) { throw "构建失败（退出码 $result）。日志：$logPath" }
    Write-Output "构建完成。日志：$logPath"
} finally {
    $env:JAVA_HOME = $previousJava
    $env:ANDROID_HOME = $previousAndroid
    $env:ANDROID_SDK_ROOT = $previousSdk
    $env:GRADLE_USER_HOME = $previousGradle
}
