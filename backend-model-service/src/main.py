"""
模型网关服务入口。
提供生成任务提交、状态查询和结果获取接口。
"""

from fastapi import FastAPI
from .routers import assets, generation, health

app = FastAPI(title="CHJ AIGC 模型网关", version="1.0.0")

app.include_router(health.router)
app.include_router(generation.router, prefix="/api/model")
app.include_router(assets.router, prefix="/api/model")
