---
name: create-task
description: Create new tasks for this project from a spec, a tech plan, or a detailed task request. Use this skill whenever the user wants actionable tasks, task breakdowns, implementation steps, or coding work items derived from specs/tech plans or specific task descriptions, including from chat or attachments. This skill should also trigger when the user asks to split a tech plan into N tasks.
---
# Create Task (Project)

You are creating tasks for this repo. Follow the project template and keep everything consistent with existing task docs.

## Before writing tasks
First, confirm details with the human. You must ask clarifying questions if any of these are missing or unclear, and pause until the user answers before creating or saving files:
- source artifact: spec or tech plan (required)
- scope boundaries and dependencies
- target files/modules and local dependencies
- acceptance criteria and required tests
- constraints and negative instructions
- number of tasks if the input is a tech plan

If the input is a single task request, confirm atomicity and independence.

## Overlap detection
Check existing tasks in `spec/tasks/` for duplicates or near-duplicates. If a similar task exists, warn the user and propose a reformulation to avoid overlap before writing.

## Sources of truth
Use the repo docs and any attachments the user references. Prioritize:
- `spec/tasks/`
- `spec/tech-plans/`
- `spec/specs/`
- any files the user names or attaches

If you need a template, read `references/task-template.md`.

## Output
Create new Markdown file(s) in `spec/tasks/` named like `task-XXX-<short-slug>.md`.
- Pick the next available numeric id (three digits). Use the highest existing id + 1.
- Use kebab-case for the slug.
- Keep ASCII only.

After saving each file, update `spec/tasks/index.md` by adding a new bullet entry using the same format as the existing index:
`- task-XXX-<short-slug>.md — <short description>`

Always include links to the referenced spec or tech plan in frontmatter.

## Writing rules
- Tasks must be atomic and independent; split if the title implies two actions.
- Use a direct, actionable title with a clear verb and component target.
- Provide local context: exact files/modules to touch and local dependencies to import.
- Describe scope as short, ordered steps.
- Include acceptance criteria with explicit success and failure cases.
- Include required tests (or explicit note if none are needed).
- Include constraints and negative instructions.

## Report structure
Follow this template exactly:
- Frontmatter (id, status, links)
- H1 title (action verb + component)
- Local Context
- Scope
- Acceptance Criteria and Tests
- Constraints and Negative Instructions

## Status handling
Set the initial status to `planned` unless the user specifies a different state.
Use status values: planned, in-progress, blocked, done.

## Tech plan breakdown
When the user provides a tech plan, propose N atomic tasks. Ensure each task is independent and can be executed with minimal repo search.
If a task would require broad repo knowledge, split it into smaller tasks with clearer file targets.

## Example
Input: "Create a task to validate JWT expiry in auth middleware."
Output: a new file under `spec/tasks/` using the template.
