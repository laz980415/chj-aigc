# Prompt Grounding

## Goal

Build prompt packets from brand-owned context rather than sending raw user prompts directly to the model.

## Inputs

- brand summary
- structured brand rules
- selected brand assets
- user intent
- capability type

## Output

The Python grounding layer produces:
- `system_prompt`
- `user_prompt`
- `context_summary`
- `audit` metadata

## Rules

- Brand tone and style must be injected into the system prompt.
- Forbidden statements and sensitive terms must be explicit.
- Required claims or positioning must be preserved.
- Selected assets must be referenced in a traceable format.
- Audit metadata must keep the selected rule ids and asset ids.

## Why This Matters

Without grounding, the platform would produce generic outputs that drift away from each advertiser's brand and legal constraints.
