# Generation Orchestration

## Goal

Coordinate grounded prompt construction with model routing and job lifecycle management across:
- copy generation
- image generation
- video generation

## Flow

1. Receive a generation request from the Java business layer.
2. Resolve the platform model alias through the model registry.
3. Require brand grounding for all creative tasks.
4. Require async-capable routing for video tasks.
5. Assemble a grounded prompt from brand context and generation intent.
6. Dispatch the provider invocation.
7. Return either:
   - immediate success for copy/image
   - pending status plus provider job id for video

## Lifecycle

- `PENDING`
- `RUNNING`
- `SUCCEEDED`
- `FAILED`

## V1 Behavior

- Copy and image generation are treated as synchronous tasks.
- Video generation is treated as asynchronous and returns a provider job id.
- Async video completion is reconciled back into the orchestrator state.
