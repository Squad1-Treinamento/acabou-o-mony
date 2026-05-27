---
name: create-spec
description: Create new specs for this project using the repo's standard spec template. Use this skill whenever the user asks to draft, refine, or generate specifications, functional requirements, acceptance criteria, BDD scenarios, or Definition of Done, including when they provide descriptions in chat or point to documents/attachments/specs. This skill should also trigger when the user wants specs derived from user stories, tickets, PDFs, or referenced files.
---
# Create Spec (Project)

You are creating specs for this repo. Follow the project template and keep everything consistent with existing spec docs.

## Before writing the spec
First, confirm details with the human. You must ask clarifying questions if any of these are missing or unclear, and pause until the user answers before creating or saving the file:
- high-level context (where this fits in the system)
- primary objective (user value)
- functional requirements and business rules
- acceptance criteria (BDD) including alternate/error flows
- tech stack constraints and design standards
- whether Interface and Data Contracts are needed

If the user wants multiple specs, confirm how to split them.

## Sources of truth
Use the repo docs and any attachments the user references. Prioritize:
- `spec/specs/`
- `spec/user-stories/`
- `spec/tech-plans/`
- any files the user names or attaches

If you need a template, read `references/spec-template.md`.

## Output
Create a new Markdown file in `spec/specs/` named like `spec-XXX-<short-slug>.md`.
- Pick the next available numeric id (three digits). Use the highest existing id + 1.
- Use kebab-case for the slug.
- Keep ASCII only.

After saving the file, update `spec/specs/index.md` by adding a new bullet entry using the same format as the existing index:
`- spec-XXX-<short-slug>.md — <short description>`

Only include a link to a user story if the human indicates one should be linked.

## Writing rules
- Be unambiguous, modular, and behavior-focused.
- Avoid code; describe expected behavior instead.
- Replace vague terms with measurable targets (e.g., "response under 200ms").
- Keep it direct and optimized for AI consumption.
- Include examples with mock inputs/outputs.

## Report structure
Follow this template exactly. If Interface and Data Contracts are not needed, omit that section.
- Frontmatter (id, status, links)
- H1 title
- Context and Primary Objective
- Functional Requirements (Behavior)
- Acceptance Criteria (BDD)
- Interface and Data Contracts (optional)
- Tech Stack and Constraints
- Examples

## Example
Input: "Spec for password reset token expiry rules."
Output: a new file under `spec/specs/` using the template.
