# Model Access Control

## Goal

Give super admins a Java-owned policy layer to decide which tenants, projects, and roles may use which platform model aliases.

## Policy Model

Each policy rule contains:
- `platformModelAlias`
- `scope`
- `effect`
- `active`
- `createdBy`
- `reason`

Scope types:
- `TENANT`
- `PROJECT`
- `ROLE`

Effects:
- `ALLOW`
- `DENY`

## Evaluation Rules

- Policies are matched against the requested platform model alias.
- Project-scoped rules have highest priority.
- Role-scoped rules are next.
- Tenant-scoped rules are lowest priority.
- If no active rule matches, access is denied by default.

## Audit Requirements

- Creating a policy rule produces an audit event.
- Disabling a policy rule produces an audit event.
- Each access decision should be traceable back to the matched rule when one exists.

## Why This Lives In Java

- Model access is a business governance concern, not just an AI adapter concern.
- Tenant, project, and role scope live in the Java business domain.
- Python workers should receive a prevalidated generation request instead of owning access policy themselves.
