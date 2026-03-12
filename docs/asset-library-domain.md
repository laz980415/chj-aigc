# Asset Library Domain

## Goal

Model advertiser clients, brands, brand rules, and brand-scoped assets as Java-owned business entities.

## Core Entities

- `Client`: advertiser customer under a tenant
- `Brand`: brand under a client
- `BrandRule`: structured brand instruction or constraint
- `Asset`: reusable material tagged by tenant, project, client, and brand

## Supported Brand Rule Types

- `TONE`
- `STYLE_GUIDE`
- `FORBIDDEN_STATEMENT`
- `REQUIRED_STATEMENT`
- `SENSITIVE_TERM`

## Supported Asset Types

- `IMAGE`
- `VIDEO`
- `DOCUMENT`
- `BRAND_GUIDE`
- `COPY_REFERENCE`

## Filtering Requirements

Assets must be retrievable by:
- tenant
- project
- client
- brand
- asset type
- tags

This is necessary because later Python prompt assembly must request only the relevant brand assets instead of scanning the whole tenant library.

## Why This Lives In Java

- Clients, brands, and assets are primary tenant business data.
- These records participate in permissions, audit, billing context, and project ownership.
- Python should consume selected context, not own the source-of-truth records.
