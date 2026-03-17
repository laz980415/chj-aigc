"""
链路追踪工具模块。
提供与 Java 微服务一致的 X-Trace-Id 传播和结构化请求日志能力。
所有 Python 服务（模型网关、AI 编排层）都应通过此模块统一处理 traceId。
"""

import logging
import time
import uuid
from contextvars import ContextVar
from functools import wraps
from typing import Callable, Optional

TRACE_HEADER = "X-Trace-Id"

# 当前请求的 traceId，通过 contextvars 在异步和同步场景下安全传递
_current_trace_id: ContextVar[str] = ContextVar("trace_id", default="")


def get_trace_id() -> str:
    """返回当前上下文的 traceId，未设置时返回空字符串。"""
    return _current_trace_id.get()


def set_trace_id(trace_id: Optional[str] = None) -> str:
    """设置当前上下文的 traceId，未传入时自动生成。返回最终使用的 traceId。"""
    tid = trace_id if trace_id and trace_id.strip() else uuid.uuid4().hex
    _current_trace_id.set(tid)
    return tid


def resolve_trace_id(incoming: Optional[str] = None) -> str:
    """从请求头解析 traceId，不存在时生成新值并写入上下文。"""
    return set_trace_id(incoming)


class TraceLogger:
    """
    带 traceId 的结构化日志记录器。
    用法：
        logger = TraceLogger(__name__)
        logger.info("处理完成", duration_ms=42)
    """

    def __init__(self, name: str):
        self._log = logging.getLogger(name)

    def _fmt(self, msg: str, **kwargs) -> str:
        tid = get_trace_id()
        parts = [f"traceId={tid}", msg]
        parts += [f"{k}={v}" for k, v in kwargs.items()]
        return " ".join(parts)

    def info(self, msg: str, **kwargs):
        self._log.info(self._fmt(msg, **kwargs))

    def warning(self, msg: str, **kwargs):
        self._log.warning(self._fmt(msg, **kwargs))

    def error(self, msg: str, **kwargs):
        self._log.error(self._fmt(msg, **kwargs))

    def debug(self, msg: str, **kwargs):
        self._log.debug(self._fmt(msg, **kwargs))


def trace_request(service_name: str = "python-service"):
    """
    函数装饰器：自动记录请求开始、完成和异常日志，并传播 traceId。
    适用于 AI 编排层的入口函数。

    用法：
        @trace_request("model-gateway")
        def handle(request, trace_id=None):
            ...
    """
    def decorator(fn: Callable) -> Callable:
        logger = TraceLogger(fn.__module__)

        @wraps(fn)
        def wrapper(*args, trace_id: Optional[str] = None, **kwargs):
            tid = resolve_trace_id(trace_id)
            started = time.monotonic()
            logger.info(f"{service_name} 请求开始", func=fn.__name__)
            try:
                result = fn(*args, **kwargs)
                duration_ms = int((time.monotonic() - started) * 1000)
                logger.info(f"{service_name} 请求完成", func=fn.__name__, duration_ms=duration_ms)
                return result
            except Exception as exc:
                duration_ms = int((time.monotonic() - started) * 1000)
                logger.error(
                    f"{service_name} 请求异常",
                    func=fn.__name__,
                    duration_ms=duration_ms,
                    error=str(exc),
                )
                raise

        return wrapper
    return decorator
