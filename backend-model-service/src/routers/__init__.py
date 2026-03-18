from .health import router as health_router
from .generation import router as generation_router
from . import assets, generation, health

__all__ = ["assets", "generation", "health"]
