from __future__ import annotations

import unittest

from platform_core.models import (
    CapabilityType,
    ModelRegistry,
    ModelStatus,
    PlatformModel,
    Provider,
    ProviderCredentialRef,
    ProviderModelBinding,
    seed_default_registry,
)


class ModelRegistryTests(unittest.TestCase):
    def test_seeded_registry_lists_models_by_capability(self) -> None:
        registry = seed_default_registry()

        models = registry.list_models_by_capability(CapabilityType.IMAGE_GENERATION)

        aliases = {model.alias for model in models}
        self.assertIn("image-standard", aliases)
        self.assertIn("image-dalle3", aliases)

    def test_registry_resolves_highest_priority_binding(self) -> None:
        registry = ModelRegistry()
        registry.add_provider(
            Provider(id="provider-a", name="Provider A", api_base_url="https://a.example.com"),
            ProviderCredentialRef(provider_id="provider-a", secret_name="KEY_A"),
        )
        registry.add_provider(
            Provider(id="provider-b", name="Provider B", api_base_url="https://b.example.com"),
            ProviderCredentialRef(provider_id="provider-b", secret_name="KEY_B"),
        )
        registry.add_platform_model(
            PlatformModel(
                id="pm-1",
                alias="copy-standard",
                display_name="Copy Standard",
                capability=CapabilityType.COPYWRITING,
                metering_dimensions=(),
            )
        )
        registry.bind_provider_model(
            ProviderModelBinding(
                id="b-2",
                platform_model_id="pm-1",
                provider_id="provider-b",
                provider_model_name="model-b",
                priority=20,
            )
        )
        registry.bind_provider_model(
            ProviderModelBinding(
                id="b-1",
                platform_model_id="pm-1",
                provider_id="provider-a",
                provider_model_name="model-a",
                priority=10,
            )
        )

        route = registry.resolve_route("copy-standard", CapabilityType.COPYWRITING)

        self.assertEqual(route.provider.id, "provider-a")
        self.assertEqual(route.binding.provider_model_name, "model-a")

    def test_registry_filters_by_async_capability(self) -> None:
        registry = seed_default_registry()

        route = registry.resolve_route(
            "video-standard",
            CapabilityType.VIDEO_GENERATION,
            require_async_jobs=True,
        )

        self.assertEqual(route.binding.provider_model_name, "sora-2")

    def test_registry_ignores_disabled_platform_models(self) -> None:
        registry = ModelRegistry()
        registry.add_provider(
            Provider(id="provider-a", name="Provider A", api_base_url="https://a.example.com"),
            ProviderCredentialRef(provider_id="provider-a", secret_name="KEY_A"),
        )
        registry.add_platform_model(
            PlatformModel(
                id="pm-disabled",
                alias="copy-standard",
                display_name="Disabled",
                capability=CapabilityType.COPYWRITING,
                metering_dimensions=(),
                status=ModelStatus.DEPRECATED,
            )
        )

        with self.assertRaises(LookupError):
            registry.find_platform_model("copy-standard", CapabilityType.COPYWRITING)


if __name__ == "__main__":
    unittest.main()
