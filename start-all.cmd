@echo off
set JAVA_HOME=D:\ProgramFiles\jdk\jdk21
set PATH=%JAVA_HOME%\bin;%PATH%

echo [1/3] 启动认证服务 (8083)...
start "Auth Service" cmd /k "set JAVA_HOME=D:\ProgramFiles\jdk\jdk21 && set PATH=D:\ProgramFiles\jdk\jdk21\bin;%PATH% && cd /d E:\ai-workspaces\chj-aigc-auth-service && mvn spring-boot:run"

echo 等待认证服务启动 (20秒)...
timeout /t 20 /nobreak

echo [2/3] 启动平台服务 (8080)...
start "Platform Service" cmd /k "set JAVA_HOME=D:\ProgramFiles\jdk\jdk21 && set PATH=D:\ProgramFiles\jdk\jdk21\bin;%PATH% && cd /d E:\ai-workspaces\chj-aigc-platform-service && mvn spring-boot:run"

echo [3/3] 启动租户服务 (8082)...
start "Tenant Service" cmd /k "set JAVA_HOME=D:\ProgramFiles\jdk\jdk21 && set PATH=D:\ProgramFiles\jdk\jdk21\bin;%PATH% && cd /d E:\ai-workspaces\backend-tenant-service && mvn spring-boot:run"

echo 所有服务已在独立窗口启动。
echo 认证服务: http://localhost:8083/api/auth/health
echo 平台服务: http://localhost:8080/actuator/health
echo 租户服务: http://localhost:8082/actuator/health
pause
