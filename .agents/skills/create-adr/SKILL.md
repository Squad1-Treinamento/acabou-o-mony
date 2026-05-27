---
name: create-adr
description: Create new ADRs for this project. Use this skill whenever the user asks to record architectural decisions, technology choices, code standards, structural definitions, or new service introductions. If any of those triggers appear, ask whether to proceed with ADR documentation. This skill also triggers when the user wants to formalize decisions from specs, tech plans, or attachments.
---
# Create ADR (Project)

You are creating ADRs for this repo. ADRs are decision records, not implementation guides. Write in pt-BR, direct and technical.

## Trigger check
If the request implies a new architectural decision, technology choice, code standard, structural definition, or new service, ask the user if an ADR should be created before proceeding.

## Before writing the ADR
First, confirm details with the human. You must ask clarifying questions if any of these are missing or unclear, and pause until the user answers before creating or saving the file:
- the problem being solved (Contexto)
- the decision made and how it works (Decisao)
- the impacts (Consequencias)
- status (one of: Proposta | Aceita | Deprecada | Substituida)
- whether any assets (diagrams/images) should be included

If the user does not choose a status, include all status options in the ADR for them to fill.

## Overlap detection
Check existing ADRs in `spec/adrs/` for duplicates or near-duplicates. If a similar ADR exists, warn the user and propose a reformulation before writing.

## Sources of truth
Use the repo docs and any attachments the user references. Prioritize:
- `spec/adrs/`
- `spec/specs/`
- `spec/tech-plans/`
- any files the user names or attaches

If you need a template, read `references/adr-template.md`.

## Output
Create a new Markdown file in `spec/adrs/` named like `adr-XXX-<short-slug>.md`.
- Pick the next available numeric id (three digits). Use the highest existing id + 1.
- Use kebab-case for the slug.
- Keep ASCII only.

After saving the file, update `spec/adrs/index.md` by adding a new bullet entry using the same format as the existing index:
`- adr-XXX-<short-slug>.md — <short description>`

## Writing rules
- Use direct language, no personal opinion.
- Avoid code details, step-by-step tutorials, or excessive didactic text.
- Keep it focused on the decision, not the debate.
- Include both positive and negative consequences.
- Keep it concise and avoid long ADRs.

## Report structure
Follow this template exactly:
- Frontmatter (id, status, links)
- H1 title: "ADR-XXX - Titulo da decisao"
- Status
- Contexto
- Decisao
- Consequencias

## Checklist before saving
- Is the problem clear?
- Is the decision clear and implementable without code?
- Are impacts explicit (positive and negative)?
- Is it aligned with existing ADRs?

## Example
Input: "Decidimos usar PostgreSQL 15 em vez de MySQL."
Output: a new file under `spec/adrs/` using the template.
