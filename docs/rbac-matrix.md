# RBAC Matrix

## Role Model

The initial permission model uses three scope levels:

- `platform`: global operator scope
- `tenant`: organization scope
- `project`: workspace scope inside a tenant

## Built-In Roles

### `platform_super_admin`

Scope: `platform`

Permissions:
- `platform.tenant.manage`
- `platform.model.manage`
- `platform.pricing.manage`
- `audit.view`

### `tenant_owner`

Scope: `tenant`

Permissions:
- `tenant.member.manage`
- `tenant.project.manage`
- `tenant.wallet.manage`
- `asset.manage`
- `audit.view`
- `generation.use`

### `tenant_member`

Scope: `tenant`

Permissions:
- `generation.use`

### `project_admin`

Scope: `project`

Permissions:
- `project.member.manage`
- `project.brand.manage`
- `asset.manage`
- `generation.use`

### `project_user`

Scope: `project`

Permissions:
- `generation.use`

## Inheritance Rules

- Platform permissions are checked first.
- Tenant permissions apply to all projects in the same tenant when the same permission exists at tenant scope.
- Project permissions only apply inside the assigned project.
- A user may have both tenant and project assignments.

## Example Scenarios

- A `tenant_owner` can manage tenant members and all tenant projects, but cannot manage platform pricing.
- A `project_admin` can manage project members and project brand settings, but cannot recharge the tenant wallet.
- A `project_user` can create generation tasks for the assigned project, but cannot change project membership.
- A `platform_super_admin` can manage tenant lifecycle and model availability globally without being a tenant member.
