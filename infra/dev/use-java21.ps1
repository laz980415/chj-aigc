${preferredJavaHome} = "D:\ProgramFiles\jdk\jdk21"
${preferredMavenHome} = "D:\ProgramFiles\apache-maven-3.9.1"

${javaHomeInvalid} = [string]::IsNullOrWhiteSpace($env:JAVA_HOME) `
    -or -not (Test-Path (Join-Path $env:JAVA_HOME "bin\javac.exe")) `
    -or $env:JAVA_HOME -like "*jdk8*"

if ($javaHomeInvalid -and (Test-Path (Join-Path $preferredJavaHome "bin\javac.exe"))) {
    $env:JAVA_HOME = $preferredJavaHome
}

if (-not [string]::IsNullOrWhiteSpace($env:JAVA_HOME)) {
    $javaBin = Join-Path $env:JAVA_HOME "bin"
    if (-not ($env:Path -split ';' | Where-Object { $_ -eq $javaBin })) {
        $env:Path = "$javaBin;$env:Path"
    }
}

if (Test-Path (Join-Path $preferredMavenHome "bin\mvn.cmd")) {
    $mavenBin = Join-Path $preferredMavenHome "bin"
    if (-not ($env:Path -split ';' | Where-Object { $_ -eq $mavenBin })) {
        $env:Path = "$mavenBin;$env:Path"
    }
}
