"""Asset ingestion and retrieval endpoints."""

from __future__ import annotations

from pathlib import Path

from fastapi import APIRouter
from pydantic import BaseModel, Field

from ..asset_grounding import AssetGroundingService, GroundingAssetRef
from ..asset_ingestion import (
    AssetContextRetriever,
    AssetDescriptor,
    JsonlAssetChunkIndex,
    DeterministicEmbeddingProvider,
    build_default_asset_ingestion_service,
)


router = APIRouter()
_index_path = Path(__file__).resolve().parent.parent / "generated" / "asset_chunks.jsonl"
_ingestion_service = build_default_asset_ingestion_service(index_path=_index_path)
_retriever = AssetContextRetriever(DeterministicEmbeddingProvider(), JsonlAssetChunkIndex(_index_path))
_grounding_service = AssetGroundingService(_retriever)


class IngestAssetRequest(BaseModel):
    asset_id: str
    tenant_id: str
    project_id: str
    brand_id: str
    name: str
    kind: str
    uri: str
    tags: list[str] = Field(default_factory=list)
    summary: str = ""


class AssetChunkResponse(BaseModel):
    chunk_id: str
    asset_id: str
    tenant_id: str
    project_id: str
    brand_id: str
    asset_kind: str
    source_uri: str
    content_text: str
    summary: str
    page_no: int | None = None
    frame_no: int | None = None


class SearchAssetChunksRequest(BaseModel):
    query: str
    tenant_id: str
    brand_id: str | None = None
    asset_ids: list[str] = Field(default_factory=list)
    limit: int = 5


class GroundingAssetInput(BaseModel):
    asset_id: str
    name: str
    kind: str
    uri: str
    tags: list[str] = Field(default_factory=list)
    summary: str = ""


class BuildGroundingContextRequest(BaseModel):
    tenant_id: str
    brand_id: str
    user_prompt: str
    assets: list[GroundingAssetInput] = Field(default_factory=list)
    limit: int = 5


@router.post("/assets/ingest")
def ingest_asset(request: IngestAssetRequest):
    chunks = _ingestion_service.ingest(
        AssetDescriptor(
            asset_id=request.asset_id,
            tenant_id=request.tenant_id,
            project_id=request.project_id,
            brand_id=request.brand_id,
            name=request.name,
            kind=request.kind,
            uri=request.uri,
            tags=tuple(request.tags),
            summary=request.summary,
        )
    )
    return {
        "asset_id": request.asset_id,
        "chunk_count": len(chunks),
        "chunks": [serialize_chunk(chunk) for chunk in chunks],
    }


@router.post("/assets/search")
def search_asset_chunks(request: SearchAssetChunksRequest):
    chunks = _retriever.retrieve(
        request.query,
        tenant_id=request.tenant_id,
        brand_id=request.brand_id,
        asset_ids=tuple(request.asset_ids),
        limit=request.limit,
    )
    return {
        "total": len(chunks),
        "items": [serialize_chunk(chunk) for chunk in chunks],
    }


@router.post("/assets/grounding-context")
def build_grounding_context(request: BuildGroundingContextRequest):
    context = _grounding_service.build_context(
        tenant_id=request.tenant_id,
        brand_id=request.brand_id,
        user_prompt=request.user_prompt,
        assets=tuple(
            GroundingAssetRef(
                asset_id=asset.asset_id,
                name=asset.name,
                kind=asset.kind,
                uri=asset.uri,
                tags=tuple(asset.tags),
                summary=asset.summary,
            )
            for asset in request.assets
        ),
        limit=request.limit,
    )
    return {
        "context_summary": context.context_summary,
        "snippets": [
            {
                "asset_id": snippet.asset_id,
                "asset_name": snippet.asset_name,
                "asset_kind": snippet.asset_kind,
                "source_uri": snippet.source_uri,
                "content_text": snippet.content_text,
                "summary": snippet.summary,
                "page_no": snippet.page_no,
                "frame_no": snippet.frame_no,
            }
            for snippet in context.snippets
        ],
    }


def serialize_chunk(chunk) -> dict[str, object]:
    return AssetChunkResponse(
        chunk_id=chunk.chunk_id,
        asset_id=chunk.asset_id,
        tenant_id=chunk.tenant_id,
        project_id=chunk.project_id,
        brand_id=chunk.brand_id,
        asset_kind=chunk.asset_kind,
        source_uri=chunk.source_uri,
        content_text=chunk.content_text,
        summary=chunk.summary,
        page_no=chunk.page_no,
        frame_no=chunk.frame_no,
    ).model_dump()
