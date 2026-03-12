from __future__ import annotations

import unittest

from platform_core.identity import (
    AuthorizationContext,
    IdentityGraph,
    Permission,
    Project,
    ProjectMembership,
    Tenant,
    TenantMembership,
    User,
)


class IdentityGraphTests(unittest.TestCase):
    def setUp(self) -> None:
        self.graph = IdentityGraph()
        self.graph.add_user(User(id="u-admin", email="admin@example.com", display_name="Admin"))
        self.graph.add_user(User(id="u-owner", email="owner@example.com", display_name="Owner"))
        self.graph.add_user(User(id="u-pm", email="pm@example.com", display_name="PM"))
        self.graph.add_user(User(id="u-user", email="user@example.com", display_name="User"))
        self.graph.add_tenant(Tenant(id="t-1", name="Agency One"))
        self.graph.add_project(Project(id="p-1", tenant_id="t-1", name="Brand Launch"))

    def test_platform_super_admin_has_global_permissions(self) -> None:
        self.graph.assign_platform_role("u-admin", "platform_super_admin")

        allowed = self.graph.has_permission(
            "u-admin",
            Permission.PLATFORM_MODEL_MANAGE,
            AuthorizationContext(),
        )

        self.assertTrue(allowed)

    def test_tenant_owner_has_tenant_permissions(self) -> None:
        self.graph.add_tenant_membership(
            TenantMembership(user_id="u-owner", tenant_id="t-1", role_key="tenant_owner")
        )

        self.assertTrue(
            self.graph.has_permission(
                "u-owner",
                Permission.TENANT_WALLET_MANAGE,
                AuthorizationContext(tenant_id="t-1"),
            )
        )
        self.assertFalse(
            self.graph.has_permission(
                "u-owner",
                Permission.PLATFORM_PRICING_MANAGE,
                AuthorizationContext(tenant_id="t-1"),
            )
        )

    def test_project_admin_inherits_project_scoped_permissions(self) -> None:
        self.graph.add_tenant_membership(
            TenantMembership(user_id="u-pm", tenant_id="t-1", role_key="tenant_member")
        )
        self.graph.add_project_membership(
            ProjectMembership(
                user_id="u-pm",
                tenant_id="t-1",
                project_id="p-1",
                role_key="project_admin",
            )
        )

        self.assertTrue(
            self.graph.has_permission(
                "u-pm",
                Permission.PROJECT_MEMBER_MANAGE,
                AuthorizationContext(tenant_id="t-1", project_id="p-1"),
            )
        )
        self.assertTrue(
            self.graph.has_permission(
                "u-pm",
                Permission.GENERATION_USE,
                AuthorizationContext(tenant_id="t-1", project_id="p-1"),
            )
        )

    def test_project_permissions_do_not_leak_to_other_projects(self) -> None:
        self.graph.add_project(Project(id="p-2", tenant_id="t-1", name="Other Project"))
        self.graph.add_project_membership(
            ProjectMembership(
                user_id="u-user",
                tenant_id="t-1",
                project_id="p-1",
                role_key="project_user",
            )
        )

        self.assertTrue(
            self.graph.has_permission(
                "u-user",
                Permission.GENERATION_USE,
                AuthorizationContext(tenant_id="t-1", project_id="p-1"),
            )
        )
        self.assertFalse(
            self.graph.has_permission(
                "u-user",
                Permission.GENERATION_USE,
                AuthorizationContext(tenant_id="t-1", project_id="p-2"),
            )
        )

    def test_permission_matrix_lists_role_permissions(self) -> None:
        matrix = self.graph.permission_matrix()

        self.assertIn("tenant_owner", matrix)
        self.assertIn("tenant.wallet.manage", matrix["tenant_owner"]["permissions"])


if __name__ == "__main__":
    unittest.main()
