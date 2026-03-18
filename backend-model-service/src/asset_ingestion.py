from __future__ import annotations

"""Asset parsing, embedding, and chunk indexing pipeline."""

from dataclasses import dataclass
from math import sqrt
from pathlib import Path
from typing import Protocol
import hashlib
import json
import re
import httpx


TOKEN_PATTERN = re.compile(r"[\w\u4e00-\u9fff]+", re.UNICODE)


@dataclass(frozen=True)
class AssetDescriptor:
    asset_id: str
    tenant_id: str
    project_id: str
    brand_id: str
    name: str
    kind: str
    uri: str
    tags: tuple[str, ...] = ()
    summary: str = ""


@dataclass(frozen=True)
class AssetChunk:
    chunk_id: str
    asset_id: str
    tenant_id: str
    project_id: str
    brand_id: str
    asset_kind: str
    source_uri: str
    content_text: str
    summary: str
    embedding: tuple[float, ...]
    page_no: int | None = None
    frame_no: int | None = None


class AssetParser(Protocol):
    def supports(self, kind: str) -> bool:
        ...

    def parse(self, asset: AssetDescriptor) -> list[ParsedChunk]:
        ...


class EmbeddingProvider(Protocol):
    def embed(self, text: str) -> tuple[float, ...]:
        ...


class AssetChunkIndex(Protocol):
    def upsert_chunks(self, chunks: list[AssetChunk]) -> None:
        ...

    def search(
        self,
        query_embedding: tuple[float, ...],
        *,
        tenant_id: str,
        brand_id: str | None = None,
        asset_ids: tuple[str, ...] = (),
        limit: int = 5,
    ) -> list[AssetChunk]:
        ...


@dataclass(frozen=True)
class ParsedChunk:
    content_text: str
    summary: str
    page_no: int | None = None
    frame_no: int | None = None


class DocumentAssetParser:
    def __init__(self, chunk_size: int = 240) -> None:
        self.chunk_size = chunk_size

    def supports(self, kind: str) -> bool:
        return kind.lower() in {"document", "brand_guide", "copy_reference"}

    def parse(self, asset: AssetDescriptor) -> list[ParsedChunk]:
        raw_text = self._read_text(asset)
        segments = [segment.strip() for segment in re.split(r"\n\s*\n", raw_text) if segment.strip()]
        if not segments:
            segments = [raw_text.strip()] if raw_text.strip() else []
        chunks: list[ParsedChunk] = []
        page_no = 1
        current = ""
        for segment in segments:
            candidate = segment if not current else f"{current}\n\n{segment}"
            if current and len(candidate) > self.chunk_size:
                chunks.append(
                    ParsedChunk(
                        content_text=current,
                        summary=f"{asset.name} 文档片段 {page_no}",
                        page_no=page_no,
                    )
                )
                page_no += 1
                current = segment
            else:
                current = candidate
        if current:
            chunks.append(
                ParsedChunk(
                    content_text=current,
                    summary=f"{asset.name} 文档片段 {page_no}",
                    page_no=page_no,
                )
            )
        return chunks

    def _read_text(self, asset: AssetDescriptor) -> str:
        path = Path(asset.uri)
        if path.suffix.lower() in {".txt", ".md"}:
            return path.read_text(encoding="utf-8")
        raw = path.read_bytes()
        text = raw.decode("utf-8", errors="ignore").strip()
        if text:
            return text
        return f"{asset.name}\n{asset.summary}\n{' '.join(asset.tags)}".strip()


class ImageAssetParser:
    def supports(self, kind: str) -> bool:
        return kind.lower() == "image"

    def parse(self, asset: AssetDescriptor) -> list[ParsedChunk]:
        tag_text = ", ".join(asset.tags)
        description = f"图片素材 {asset.name}"
        if asset.summary:
            description += f"，摘要：{asset.summary}"
        if tag_text:
            description += f"，标签：{tag_text}"
        description += f"，来源文件：{Path(asset.uri).name}"
        return [ParsedChunk(content_text=description, summary=f"{asset.name} 图片描述")]


class VideoAssetParser:
    def supports(self, kind: str) -> bool:
        return kind.lower() == "video"

    def parse(self, asset: AssetDescriptor) -> list[ParsedChunk]:
        tag_text = ", ".join(asset.tags)
        base = asset.summary or f"视频素材 {asset.name}"
        return [
            ParsedChunk(
                content_text=f"{base}，关键帧 1：开场主体展示。标签：{tag_text}".strip(),
                summary=f"{asset.name} 关键帧 1",
                frame_no=1,
            ),
            ParsedChunk(
                content_text=f"{base}，关键帧 2：中段场景切换。标签：{tag_text}".strip(),
                summary=f"{asset.name} 关键帧 2",
                frame_no=2,
            ),
            ParsedChunk(
                content_text=f"{base}，关键帧 3：结尾品牌露出。标签：{tag_text}".strip(),
                summary=f"{asset.name} 关键帧 3",
                frame_no=3,
            ),
        ]


class AssetParserRegistry:
    def __init__(self, parsers: tuple[AssetParser, ...]) -> None:
        self.parsers = parsers

    def parse(self, asset: AssetDescriptor) -> list[ParsedChunk]:
        for parser in self.parsers:
            if parser.supports(asset.kind):
                return parser.parse(asset)
        raise ValueError(f"Unsupported asset kind: {asset.kind}")


class DeterministicEmbeddingProvider:
    """A stable local embedding fallback for tests and offline development."""

    def __init__(self, dimension: int = 16) -> None:
        self.dimension = dimension

    def embed(self, text: str) -> tuple[float, ...]:
        vector = [0.0] * self.dimension
        tokens = TOKEN_PATTERN.findall(text.lower())
        if not tokens:
            return tuple(vector)
        for token in tokens:
            digest = hashlib.sha256(token.encode("utf-8")).digest()
            bucket = int.from_bytes(digest[:2], "big") % self.dimension
            weight = (int.from_bytes(digest[2:4], "big") % 1000) / 1000.0 + 1.0
            vector[bucket] += weight
        norm = sqrt(sum(value * value for value in vector))
        if norm == 0:
            return tuple(vector)
        return tuple(value / norm for value in vector)


class InMemoryAssetChunkIndex:
    def __init__(self) -> None:
        self._chunks: dict[str, AssetChunk] = {}

    def upsert_chunks(self, chunks: list[AssetChunk]) -> None:
        for chunk in chunks:
            self._chunks[chunk.chunk_id] = chunk

    def search(
        self,
        query_embedding: tuple[float, ...],
        *,
        tenant_id: str,
        brand_id: str | None = None,
        asset_ids: tuple[str, ...] = (),
        limit: int = 5,
    ) -> list[AssetChunk]:
        asset_id_filter = set(asset_ids)
        scored = []
        for chunk in self._chunks.values():
            if chunk.tenant_id != tenant_id:
                continue
            if brand_id and chunk.brand_id != brand_id:
                continue
            if asset_id_filter and chunk.asset_id not in asset_id_filter:
                continue
            scored.append((cosine_similarity(query_embedding, chunk.embedding), chunk))
        scored.sort(key=lambda item: item[0], reverse=True)
        return [chunk for _, chunk in scored[:limit]]


class JsonlAssetChunkIndex:
    """A local JSONL-backed index for development until Elasticsearch is wired in."""

    def __init__(self, path: str | Path) -> None:
        self.path = Path(path)
        self.path.parent.mkdir(parents=True, exist_ok=True)

    def upsert_chunks(self, chunks: list[AssetChunk]) -> None:
        existing = {chunk.chunk_id: chunk for chunk in self._read_all()}
        for chunk in chunks:
            existing[chunk.chunk_id] = chunk
        with self.path.open("w", encoding="utf-8") as handle:
            for chunk in existing.values():
                handle.write(json.dumps(chunk_to_dict(chunk), ensure_ascii=False) + "\n")

    def search(
        self,
        query_embedding: tuple[float, ...],
        *,
        tenant_id: str,
        brand_id: str | None = None,
        asset_ids: tuple[str, ...] = (),
        limit: int = 5,
    ) -> list[AssetChunk]:
        return InMemoryAssetChunkIndexSearchAdapter(self._read_all()).search(
            query_embedding,
            tenant_id=tenant_id,
            brand_id=brand_id,
            asset_ids=asset_ids,
            limit=limit,
        )

    def _read_all(self) -> list[AssetChunk]:
        if not self.path.exists():
            return []
        chunks: list[AssetChunk] = []
        for line in self.path.read_text(encoding="utf-8").splitlines():
            if not line.strip():
                continue
            payload = json.loads(line)
            chunks.append(
                AssetChunk(
                    chunk_id=payload["chunk_id"],
                    asset_id=payload["asset_id"],
                    tenant_id=payload["tenant_id"],
                    project_id=payload["project_id"],
                    brand_id=payload["brand_id"],
                    asset_kind=payload["asset_kind"],
                    source_uri=payload["source_uri"],
                    content_text=payload["content_text"],
                    summary=payload["summary"],
                    embedding=tuple(payload["embedding"]),
                    page_no=payload.get("page_no"),
                    frame_no=payload.get("frame_no"),
                )
            )
        return chunks


class ElasticsearchAssetChunkIndex:
    """Elasticsearch-backed chunk index for production-sized asset retrieval."""

    def __init__(self, base_url: str, index_name: str = "asset_chunks") -> None:
        self.base_url = base_url.rstrip("/")
        self.index_name = index_name

    def upsert_chunks(self, chunks: list[AssetChunk]) -> None:
        if not chunks:
            return
        lines: list[str] = []
        for chunk in chunks:
            lines.append(json.dumps({"index": {"_index": self.index_name, "_id": chunk.chunk_id}}))
            lines.append(json.dumps(chunk_to_dict(chunk), ensure_ascii=False))
        payload = "\n".join(lines) + "\n"
        with httpx.Client(timeout=30) as client:
            response = client.post(
                f"{self.base_url}/_bulk",
                headers={"Content-Type": "application/x-ndjson"},
                content=payload.encode("utf-8"),
            )
            response.raise_for_status()

    def search(
        self,
        query_embedding: tuple[float, ...],
        *,
        tenant_id: str,
        brand_id: str | None = None,
        asset_ids: tuple[str, ...] = (),
        limit: int = 5,
    ) -> list[AssetChunk]:
        filters: list[dict[str, object]] = [{"term": {"tenant_id": tenant_id}}]
        if brand_id:
            filters.append({"term": {"brand_id": brand_id}})
        if asset_ids:
            filters.append({"terms": {"asset_id": list(asset_ids)}})
        payload = {
            "size": limit,
            "query": {
                "script_score": {
                    "query": {"bool": {"filter": filters}},
                    "script": {
                        "source": "cosineSimilarity(params.query_vector, 'embedding') + 1.0",
                        "params": {"query_vector": list(query_embedding)},
                    },
                }
            },
        }
        with httpx.Client(timeout=30) as client:
            response = client.post(f"{self.base_url}/{self.index_name}/_search", json=payload)
            response.raise_for_status()
            data = response.json()
        hits = data.get("hits", {}).get("hits", [])
        return [chunk_from_dict(hit["_source"]) for hit in hits]


class InMemoryAssetChunkIndexSearchAdapter(InMemoryAssetChunkIndex):
    def __init__(self, chunks: list[AssetChunk]) -> None:
        super().__init__()
        self.upsert_chunks(chunks)


class AssetIngestionService:
    def __init__(
        self,
        parser_registry: AssetParserRegistry,
        embedding_provider: EmbeddingProvider,
        chunk_index: AssetChunkIndex,
    ) -> None:
        self.parser_registry = parser_registry
        self.embedding_provider = embedding_provider
        self.chunk_index = chunk_index

    def ingest(self, asset: AssetDescriptor) -> list[AssetChunk]:
        parsed_chunks = self.parser_registry.parse(asset)
        chunks = [
            AssetChunk(
                chunk_id=f"{asset.asset_id}-chunk-{index + 1}",
                asset_id=asset.asset_id,
                tenant_id=asset.tenant_id,
                project_id=asset.project_id,
                brand_id=asset.brand_id,
                asset_kind=asset.kind,
                source_uri=asset.uri,
                content_text=item.content_text,
                summary=item.summary,
                embedding=self.embedding_provider.embed(item.content_text),
                page_no=item.page_no,
                frame_no=item.frame_no,
            )
            for index, item in enumerate(parsed_chunks)
        ]
        self.chunk_index.upsert_chunks(chunks)
        return chunks


class AssetContextRetriever:
    def __init__(self, embedding_provider: EmbeddingProvider, chunk_index: AssetChunkIndex) -> None:
        self.embedding_provider = embedding_provider
        self.chunk_index = chunk_index

    def retrieve(
        self,
        query: str,
        *,
        tenant_id: str,
        brand_id: str | None = None,
        asset_ids: tuple[str, ...] = (),
        limit: int = 5,
    ) -> list[AssetChunk]:
        return self.chunk_index.search(
            self.embedding_provider.embed(query),
            tenant_id=tenant_id,
            brand_id=brand_id,
            asset_ids=asset_ids,
            limit=limit,
        )


def cosine_similarity(left: tuple[float, ...], right: tuple[float, ...]) -> float:
    if len(left) != len(right):
        raise ValueError("Embedding dimensions do not match")
    return sum(l * r for l, r in zip(left, right))


def chunk_to_dict(chunk: AssetChunk) -> dict[str, object]:
    return {
        "chunk_id": chunk.chunk_id,
        "asset_id": chunk.asset_id,
        "tenant_id": chunk.tenant_id,
        "project_id": chunk.project_id,
        "brand_id": chunk.brand_id,
        "asset_kind": chunk.asset_kind,
        "source_uri": chunk.source_uri,
        "content_text": chunk.content_text,
        "summary": chunk.summary,
        "embedding": list(chunk.embedding),
        "page_no": chunk.page_no,
        "frame_no": chunk.frame_no,
    }


def chunk_from_dict(payload: dict[str, object]) -> AssetChunk:
    return AssetChunk(
        chunk_id=str(payload["chunk_id"]),
        asset_id=str(payload["asset_id"]),
        tenant_id=str(payload["tenant_id"]),
        project_id=str(payload["project_id"]),
        brand_id=str(payload["brand_id"]),
        asset_kind=str(payload["asset_kind"]),
        source_uri=str(payload["source_uri"]),
        content_text=str(payload["content_text"]),
        summary=str(payload["summary"]),
        embedding=tuple(float(value) for value in payload["embedding"]),
        page_no=int(payload["page_no"]) if payload.get("page_no") is not None else None,
        frame_no=int(payload["frame_no"]) if payload.get("frame_no") is not None else None,
    )


def build_default_asset_ingestion_service(index_path: str | Path | None = None) -> AssetIngestionService:
    parser_registry = AssetParserRegistry(
        parsers=(
            DocumentAssetParser(),
            ImageAssetParser(),
            VideoAssetParser(),
        )
    )
    embedding_provider = DeterministicEmbeddingProvider()
    chunk_index: AssetChunkIndex
    if index_path is None:
        chunk_index = InMemoryAssetChunkIndex()
    else:
        chunk_index = JsonlAssetChunkIndex(index_path)
    return AssetIngestionService(parser_registry, embedding_provider, chunk_index)
