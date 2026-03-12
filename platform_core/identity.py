from __future__ import annotations

from dataclasses import dataclass, field
from enum import Enum
from typing import Iterable


class ScopeType(str, Enum):
    PLATFORM = "platform"
    TENANT = "tenant"
    PROJECT = "project"


class Permission(str, Enum):
    PLATFORM_TENANT_MANAGE = "platform.tenant.manage"
    PLATFORM_MODEL_MANAGE = "platform.model.manage"
    PLATFORM_PRICING_MANAGE = "platform.pricing.manage"
    TENANT_MEMBER_MANAGE = "tenant.member.manage"
    TENANT_PROJECT_MANAGE = "tenant.project.manage"
    TENANT_WALLET_MANAGE = "tenant.wallet.manage"
    PROJECT_MEMBER_MANAGE = "project.member.manage"
    PROJECT_BRAND_MANAGE = "project.brand.manage"
    GENERATION_USE = "generation.use"
    ASSET_MANAGE = "asset.manage"
    AUDIT_VIEW = "audit.view"


@dataclass(frozen=True)
class RoleDefinition:
    key: str
    name: str
    scope_type: ScopeType
    permissions: frozenset[Permission]
    description: str = ""


@dataclass(frozen=True)
class User:
    id: str
    email: str
    display_name: str
    active: bool = True


@dataclass(frozen=True)
class Tenant:
    id: str
    name: str
    active: bool = True


@dataclass(frozen=True)
class Project:
    id: str
    tenant_id: str
    name: str
    active: bool = True


@dataclass(frozen=True)
class PlatformRoleAssignment:
    user_id: str
    role_key: str


@dataclass(frozen=True)
class TenantMembership:
    user_id: str
    tenant_id: str
    role_key: str
    active: bool = True


@dataclass(frozen=True)
class ProjectMembership:
    user_id: str
    tenant_id: str
    project_id: str
    role_key: str
    active: bool = True


@dataclass(frozen=True)
class AuthorizationContext:
    tenant_id: str | None = None
    project_id: str | None = None


def builtin_roles() -> dict[str, RoleDefinition]:
    return {
        "platform_super_admin": RoleDefinition(
            key="platform_super_admin",
            name="Platform Super Admin",
            scope_type=ScopeType.PLATFORM,
            permissions=frozenset(
                {
                    Permission.PLATFORM_TENANT_MANAGE,
                    Permission.PLATFORM_MODEL_MANAGE,
                    Permission.PLATFORM_PRICING_MANAGE,
                    Permission.AUDIT_VIEW,
                }
            ),
            description="Global operator role outside tenant scope.",
        ),
        "tenant_owner": RoleDefinition(
            key="tenant_owner",
            name="Tenant Owner",
            scope_type=ScopeType.TENANT,
            permissions=frozenset(
                {
                    Permission.TENANT_MEMBER_MANAGE,
                    Permission.TENANT_PROJECT_MANAGE,
                    Permission.TENANT_WALLET_MANAGE,
                    Permission.ASSET_MANAGE,
                    Permission.AUDIT_VIEW,
                    Permission.GENERATION_USE,
                }
            ),
            description="Top-level tenant administrator.",
        ),
        "tenant_member": RoleDefinition(
            key="tenant_member",
            name="Tenant Member",
            scope_type=ScopeType.TENANT,
            permissions=frozenset(
                {
                    Permission.GENERATION_USE,
                }
            ),
            description="Default tenant-level participant.",
        ),
        "project_admin": RoleDefinition(
            key="project_admin",
            name="Project Admin",
            scope_type=ScopeType.PROJECT,
            permissions=frozenset(
                {
                    Permission.PROJECT_MEMBER_MANAGE,
                    Permission.PROJECT_BRAND_MANAGE,
                    Permission.ASSET_MANAGE,
                    Permission.GENERATION_USE,
                }
            ),
            description="Project-scoped administrator.",
        ),
        "project_user": RoleDefinition(
            key="project_user",
            name="Project User",
            scope_type=ScopeType.PROJECT,
            permissions=frozenset(
                {
                    Permission.GENERATION_USE,
                }
            ),
            description="Project-scoped creator role.",
        ),
    }


@dataclass
class IdentityGraph:
    users: dict[str, User] = field(default_factory=dict)
    tenants: dict[str, Tenant] = field(default_factory=dict)
    projects: dict[str, Project] = field(default_factory=dict)
    roles: dict[str, RoleDefinition] = field(default_factory=builtin_roles)
    platform_role_assignments: list[PlatformRoleAssignment] = field(default_factory=list)
    tenant_memberships: list[TenantMembership] = field(default_factory=list)
    project_memberships: list[ProjectMembership] = field(default_factory=list)

    def add_user(self, user: User) -> None:
        self.users[user.id] = user

    def add_tenant(self, tenant: Tenant) -> None:
        self.tenants[tenant.id] = tenant

    def add_project(self, project: Project) -> None:
        if project.tenant_id not in self.tenants:
            raise ValueError(f"Unknown tenant for project: {project.tenant_id}")
        self.projects[project.id] = project

    def assign_platform_role(self, user_id: str, role_key: str) -> None:
        self._require_user(user_id)
        self._require_role(role_key, ScopeType.PLATFORM)
        self.platform_role_assignments.append(PlatformRoleAssignment(user_id, role_key))

    def add_tenant_membership(self, membership: TenantMembership) -> None:
        self._require_user(membership.user_id)
        self._require_tenant(membership.tenant_id)
        self._require_role(membership.role_key, ScopeType.TENANT)
        self.tenant_memberships.append(membership)

    def add_project_membership(self, membership: ProjectMembership) -> None:
        self._require_user(membership.user_id)
        self._require_tenant(membership.tenant_id)
        project = self.projects.get(membership.project_id)
        if not project:
            raise ValueError(f"Unknown project: {membership.project_id}")
        if project.tenant_id != membership.tenant_id:
            raise ValueError("Project membership tenant does not match project tenant")
        self._require_role(membership.role_key, ScopeType.PROJECT)
        self.project_memberships.append(membership)

    def has_permission(
        self,
        user_id: str,
        permission: Permission,
        context: AuthorizationContext | None = None,
    ) -> bool:
        context = context or AuthorizationContext()
        user = self.users.get(user_id)
        if not user or not user.active:
            return False

        if self._has_platform_permission(user_id, permission):
            return True

        if context.project_id:
            project = self.projects.get(context.project_id)
            if not project or not project.active:
                return False
            if context.tenant_id and project.tenant_id != context.tenant_id:
                return False
            return self._has_project_permission(user_id, permission, project.id, project.tenant_id)

        if context.tenant_id:
            tenant = self.tenants.get(context.tenant_id)
            if not tenant or not tenant.active:
                return False
            return self._has_tenant_permission(user_id, permission, tenant.id)

        return False

    def effective_permissions(
        self,
        user_id: str,
        context: AuthorizationContext | None = None,
    ) -> set[Permission]:
        context = context or AuthorizationContext()
        permissions: set[Permission] = set()
        permissions.update(self._collect_permissions(self.platform_role_assignments_for(user_id)))

        if context.tenant_id:
            permissions.update(
                self._collect_permissions(
                    m.role_key
                    for m in self.tenant_memberships
                    if m.user_id == user_id and m.tenant_id == context.tenant_id and m.active
                )
            )

        if context.project_id:
            project = self.projects.get(context.project_id)
            if project:
                permissions.update(
                    self._collect_permissions(
                        m.role_key
                        for m in self.project_memberships
                        if m.user_id == user_id
                        and m.project_id == context.project_id
                        and m.tenant_id == project.tenant_id
                        and m.active
                    )
                )

        return permissions

    def permission_matrix(self) -> dict[str, dict[str, list[str]]]:
        matrix: dict[str, dict[str, list[str]]] = {}
        for role_key, role in self.roles.items():
            matrix[role_key] = {
                "scope_type": role.scope_type.value,
                "permissions": sorted(permission.value for permission in role.permissions),
            }
        return matrix

    def _has_platform_permission(self, user_id: str, permission: Permission) -> bool:
        return permission in self._collect_permissions(self.platform_role_assignments_for(user_id))

    def _has_tenant_permission(self, user_id: str, permission: Permission, tenant_id: str) -> bool:
        tenant_role_keys = [
            m.role_key
            for m in self.tenant_memberships
            if m.user_id == user_id and m.tenant_id == tenant_id and m.active
        ]
        return permission in self._collect_permissions(tenant_role_keys)

    def _has_project_permission(
        self,
        user_id: str,
        permission: Permission,
        project_id: str,
        tenant_id: str,
    ) -> bool:
        if self._has_tenant_permission(user_id, permission, tenant_id):
            return True
        project_role_keys = [
            m.role_key
            for m in self.project_memberships
            if m.user_id == user_id
            and m.project_id == project_id
            and m.tenant_id == tenant_id
            and m.active
        ]
        return permission in self._collect_permissions(project_role_keys)

    def _collect_permissions(self, role_keys: Iterable[str]) -> set[Permission]:
        permissions: set[Permission] = set()
        for role_key in role_keys:
            role = self.roles[role_key]
            permissions.update(role.permissions)
        return permissions

    def platform_role_assignments_for(self, user_id: str) -> list[str]:
        return [
            assignment.role_key
            for assignment in self.platform_role_assignments
            if assignment.user_id == user_id
        ]

    def _require_user(self, user_id: str) -> None:
        if user_id not in self.users:
            raise ValueError(f"Unknown user: {user_id}")

    def _require_tenant(self, tenant_id: str) -> None:
        if tenant_id not in self.tenants:
            raise ValueError(f"Unknown tenant: {tenant_id}")

    def _require_role(self, role_key: str, scope_type: ScopeType) -> None:
        role = self.roles.get(role_key)
        if not role:
            raise ValueError(f"Unknown role: {role_key}")
        if role.scope_type != scope_type:
            raise ValueError(
                f"Role {role_key} has scope {role.scope_type.value}, expected {scope_type.value}"
            )
