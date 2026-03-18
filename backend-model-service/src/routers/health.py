"""健康检查接口。"""

from fastapi import APIRouter

router = APIRouter()


@router.get("/api/model/health")
def health():
    return {"status": "ok", "service": "backend-model-service"}
