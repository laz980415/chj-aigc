param(
    [string]$Version = "2.4.1",
    [string]$TargetDir = "E:\ai-workspaces\infra\nacos\local",
    [string]$JavaHome = ""
)

$ErrorActionPreference = "Stop"
$nacosHome = Join-Path $TargetDir "nacos-server-$Version\nacos\bin"
$startup = Join-Path $nacosHome "startup.cmd"

if (-not (Test-Path $startup)) {
    Write-Error "未找到 Nacos 启动文件，请先运行 download-nacos.ps1"
}

if ([string]::IsNullOrWhiteSpace($JavaHome)) {
    $candidateJavaHomes = @(
        "D:\ProgramFiles\jdk\jdk8",
        "D:\ProgramFiles\jdk\jdk11",
        "D:\ProgramFiles\jdk\jdk17"
    )
    foreach ($candidate in $candidateJavaHomes) {
        if (Test-Path (Join-Path $candidate "bin\java.exe")) {
            $JavaHome = $candidate
            break
        }
    }
}

if ([string]::IsNullOrWhiteSpace($JavaHome)) {
    Write-Error "未找到可用的 Java 运行环境。请传入 -JavaHome，建议优先使用 JDK 8。"
}

$env:JAVA_HOME = $JavaHome
Start-Process -FilePath "cmd.exe" -ArgumentList "/c", "startup.cmd", "-m", "standalone" -WorkingDirectory $nacosHome
Write-Host "Nacos 已尝试启动，当前 JAVA_HOME=$JavaHome，请稍后访问 http://127.0.0.1:8848/nacos"
