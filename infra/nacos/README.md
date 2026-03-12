# Nacos 本地开发服务

这个目录提供两种本地开发方式：

- 无 Docker 的本地解压启动
- Docker Compose 启动

## 无 Docker 启动

先下载并解压：

```powershell
Set-Location E:\ai-workspaces\infra\nacos
.\download-nacos.ps1
```

启动：

```powershell
Set-Location E:\ai-workspaces\infra\nacos
.\start-nacos.ps1
```

停止：

```powershell
Set-Location E:\ai-workspaces\infra\nacos
.\stop-nacos.ps1
```

默认会下载并使用 `Nacos 2.4.1`。
本地 `standalone` 模式不需要额外创建 Nacos 数据库。

脚本会优先尝试这些 JDK：

- `D:\ProgramFiles\jdk\jdk8`
- `D:\ProgramFiles\jdk\jdk11`
- `D:\ProgramFiles\jdk\jdk17`

如果你要显式指定 Java，也可以这样启动：

```powershell
Set-Location E:\ai-workspaces\infra\nacos
.\start-nacos.ps1 -JavaHome "D:\ProgramFiles\jdk\jdk8"
```

注意：

- 这里不建议用 `JDK 21` 启动 Nacos 2.4.1
- 当前这套本地开发模式走的是 Nacos 自带存储，不需要你额外新建 PostgreSQL 库
- 如果后面要做 Nacos 集群和持久化，通常接的是 `MySQL`，不是当前业务库 PostgreSQL

## Docker 启动

```powershell
Set-Location E:\ai-workspaces\infra\nacos
docker compose up -d
```

启动后访问：

```text
http://127.0.0.1:8848/nacos
```

当前开发环境关闭了鉴权，适合本地联调，不适合生产。

## Docker 停止

```powershell
Set-Location E:\ai-workspaces\infra\nacos
docker compose down
```

## 配合微服务使用

启动 Nacos 后，把三个 Java 服务都改成开启注册发现：

```powershell
$env:NACOS_DISCOVERY_ENABLED="true"
$env:NACOS_SERVER_ADDR="127.0.0.1:8848"
```

如果要让网关通过服务发现转发，而不是本地直连地址，再补这两个变量：

```powershell
$env:PLATFORM_SERVICE_URI="lb://chj-aigc-platform-service"
$env:TENANT_SERVICE_URI="lb://chj-aigc-tenant-service"
```
