---
name: create-tech-plan
description: Create new tech plans for this project using the repo's standard template. Use this skill whenever the user asks to draft, refine, or generate technical plans, architecture blueprints, stack decisions, data flow plans, or security/performance approaches, including when they provide descriptions in chat or point to documents/attachments/specs. This skill should also trigger when the user wants tech plans derived from specs, user stories, tickets, PDFs, or referenced files.
---
# Create Tech Plan (Project)

You are creating tech plans for this repo. Follow the project template and keep everything consistent with existing tech plan docs.

## Before writing the plan
First, confirm details with the human. You must ask clarifying questions if any of these are missing or unclear, and pause until the user answers before creating or saving the file:
- the spec or user story this plan is based on (required)
- architecture overview and data flow
- stack/runtime versions and allowed dependencies
- design patterns and code conventions
- persistence and data modeling
- non-functional and security requirements
- which optional sections should be included

If the user wants multiple plans, confirm how to split them.

## Overlap with specs
Small overlap is acceptable. Avoid large rewrites of spec content. When details are already in a spec, reference it instead of duplicating.

## Sources of truth
Use the repo docs and any attachments the user references. Prioritize:
- `spec/tech-plans/`
- `spec/specs/`
- `spec/user-stories/`
- `spec/tasks/`
- any files the user names or attaches

If you need a template, read `references/tech-plan-template.md`.

## Output
Create a new Markdown file in `spec/tech-plans/` named like `plan-XXX-<short-slug>.md`.
- Pick the next available numeric id (three digits). Use the highest existing id + 1.
- Use kebab-case for the slug.
- Keep ASCII only.

After saving the file, update `spec/tech-plans/index.md` by adding a new bullet entry using the same format as the existing index:
`- plan-XXX-<short-slug>.md — <short description>`

Always include links to the referenced spec or user story in frontmatter.

## Writing rules
- Be unambiguous, modular, and architecture-focused.
- Avoid code; describe expected behavior and structure instead.
- Replace vague terms with measurable targets (e.g., "p95 under 200ms").
- Keep it direct and optimized for AI consumption.
- Include examples with mock flows or payloads.
- Suggest missing sections that would improve clarity.

## Report structure
Follow this template exactly. If a section does not apply, omit it.
- Frontmatter (id, status, links)
- H1 title
- Architecture Overview and Data Flow
- Stack and Dependencies
- Design Patterns and Code Conventions
- Persistence and Data Modeling
- Non-Functional Requirements and Security
- Task Breakdown Preview
- Implementation Checklist (optional)
- Examples (optional)

## Task Breakdown Preview
Include a small foreshadowing of how this will be split into tasks. Use short bullets with a status tag.
Allowed status tags: planned, blocked, needs-research, in-progress.

## Example
Input: "Tech plan for password reset flow based on spec-012."
Output: a new file under `spec/tech-plans/` using the template.
