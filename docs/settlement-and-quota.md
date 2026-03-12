# Settlement And Quota Enforcement

## Goal

Close the loop between generation jobs and commercial controls:
- usage metering
- quota enforcement
- internal pricing
- ledger deduction

## Flow

1. A generation result is produced by the orchestration layer.
2. The settlement layer converts that result into metered usage.
3. The system maps metered usage into quota dimensions.
4. User and project quotas are checked before deduction.
5. If quota is sufficient, usage is consumed from both scopes.
6. Internal pricing computes the charge.
7. The tenant ledger records the charge against the platform model.

## V1 Usage Mapping

- copywriting:
  - metered as input and output tokens
  - quota counted against aggregate `tokens`
- image generation:
  - metered as `image_count`
- video generation:
  - metered as `video_seconds`
- every request also consumes one `daily_request`

## Notes

- Current token usage is a placeholder estimate until provider-native token counts are wired in.
- The platform ledger remains the source of truth for billing.
- Quota checks happen before ledger deduction to fail early.
