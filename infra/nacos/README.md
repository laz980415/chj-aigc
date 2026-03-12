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
