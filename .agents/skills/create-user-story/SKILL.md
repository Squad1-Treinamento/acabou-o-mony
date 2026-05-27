---
name: create-user-story
description: Create new user stories for this project using the repo's standard template. Use this skill whenever the user asks to draft, refine, or generate user stories, acceptance criteria, BDD scenarios, or Definition of Done, including when they provide descriptions in chat or point to documents/attachments/specs. This skill should also trigger when the user wants user stories derived from specs, tickets, PDFs, or any referenced files.
---
# Create User Story (Project)

You are creating user stories for this repo. Follow the project template and keep everything consistent with the existing user-story docs.

## Before writing the story
First, confirm details with the human. You must ask clarifying questions if any of these are missing or unclear, and pause until the user answers before creating or saving the file:
- user/persona
- desired outcome and why it matters
- scope boundaries (what is out of scope)
- constraints, dependencies, or assumptions
- target docs to pull context from (specs, tickets, PDFs, attachments)

If the user asked for multiple stories, confirm how to split them.

## Sources of truth
Use the repo docs and any attachments the user references. Prioritize:
- `spec/user-stories/`
- `spec/specs/`
- `spec/tasks/`
- any files the user names or attaches

If you need a template, read `references/us-template.md`.

## Output
Create a new Markdown file in `spec/user-stories/` named like `us-XXX-<short-slug>.md`.
- Pick the next available numeric id (three digits). Use the highest existing id + 1.
- Use kebab-case for the slug.
- Keep ASCII only.

After saving the file, update `spec/user-stories/index.md` by adding a new bullet entry using the same format as the existing index:
`- us-XXX-<short-slug>.md — <short description>`

Always include:
- a short title
- the user story sentence
- acceptance criteria in BDD format
- Definition of Done section
- links to related spec/specs/tasks

## Writing rules
- Keep the story user-focused and implementation-agnostic.
- Use clear, testable BDD criteria (Given/When/Then).
- DoD must be explicit and verifiable.
- Keep tone concise and professional.

## Report structure
Follow this template exactly:
- Frontmatter (id, status, links)
- H1 title
- User Story
- Context
- Scope
- Acceptance Criteria (BDD)
- Definition of Done

## Example
Input: "As a user, I want to reset my password so I can regain access."
Output: a new file under `spec/user-stories/` using the template.
