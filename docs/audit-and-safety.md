# Audit And Safety

## Goal

Provide a minimum viable observability layer across:
- admin actions
- generation request/result summaries
- charge visibility
- safety policy violations

## Java Side

Java owns:
- admin audit events
- stored safety policies
- recorded safety incidents

This is appropriate because governance and compliance are business concerns tied to tenants and admins.

## Python Side

Python produces:
- generation audit summaries
- provider attribution
- safety evaluation results against generated output

This is appropriate because Python sees the final prompt/result payloads and provider details first.

## V1 Safety Model

- Policies are simple forbidden-term checks.
- Violations are attached to generation audit records.
- Java can persist those violations as safety incidents.

## Why Summaries Instead Of Raw Payloads

- Reduces exposure of sensitive brand data.
- Keeps observability useful without storing everything verbatim.
- Leaves room for stricter retention rules later.
