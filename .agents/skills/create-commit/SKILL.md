---
name: create-commit
description: Prepare and create git commits for this repo. Use this skill whenever the user asks to commit changes, split commits, prepare commit messages, stage files, or organize changes before commit. This skill should also trigger when the user wants help grouping changes by area or context.
---
# Create Commit (Project)

You are preparing git commits. Always verify with the human before staging or committing. The push is always manual and must not be performed.

## Before any git commands
Ask for explicit permission to run git commands. If not granted, do not run `git status`, `git diff`, `git add`, or `git commit`.

## Staging policy
Always ask whether to stage all files. Wait for the user's response.
- If the user says yes, stage all.
- If the user says no or is unsure, offer an interactive file list and default to partial staging (`git add -p`).

## Commit grouping
If changes span multiple areas or contexts, suggest multiple commits and propose a grouping order.
- Group by top-level folder or feature.
- Suggest a commit order from foundational changes to surface changes.

## Commit message format
Use the format: `action: description` (max 50 characters).
Follow with bullet points describing macro changes.

## Human confirmation
Always confirm the proposed commit(s) with the user before staging or committing.

## Report structure
When proposing commits, use:
- Commit title
- Bullet list of changes
- Files included

## Example
Title: feat: add initial sdd structure
- add skills scaffolding
- add placeholder specs and indexes
Files: [list]
