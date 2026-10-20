---
name: team-git-flow
description: Plan, validate, commit, push, and prepare pull requests for this repository's multi-member workflow with truthful attribution, at most three files per work commit, and fewer than twenty files per PR.
---

# Team Git Flow

Use this skill for any request in this repository that involves assigning work, creating branches, committing, pushing, reviewing, or opening pull requests.

## Load the active plan

1. Read `../../git-workflow.md`.
2. Read `../../work-packages.md`.
3. Read exactly one active allocation plan: `../../team-plan-3-members.md` or `../../team-plan-5-members.md`.
4. Use `../../templates/git-task-template.md` for issue, PR, and handoff output.

If the active team size or current member is unknown, prepare the work but do not attribute, commit, push, or open a PR until the user identifies them.

## Preserve truthful attribution

- Treat a GitHub account as a real participant, not as a slot for distributing activity.
- Never accept or request passwords, tokens, recovery codes, or private keys.
- Never use `--author`, backdated timestamps, empty commits, or identity switching to manufacture contribution history.
- The account owner must participate, review the diff, run or witness checks, and personally authenticate before push.
- It is valid to assign approachable work such as tests, documentation, QA evidence, API examples, or review when that is the person's real contribution.

## Execute one work package at a time

Before edits, report the member, PR ID, branch, two planned commits, expected files, dependency, and acceptance criteria.

During implementation:

- keep one independently testable purpose per commit;
- stage explicit paths only;
- require 1–3 unique files in every work commit;
- require 1–19 unique files in the PR;
- stop and re-plan before creating a broken intermediate commit;
- use English Conventional Commit messages from the work package;
- preserve unrelated user changes.

After implementation, run relevant tests, `git diff --check`, file-count gates, and the dry-merge conflict check from `../../git-workflow.md`.

Do not commit, push, open, approve, or merge a PR unless the user has explicitly requested that external mutation for the current work.
When push is authorized, verify the active identity without exposing credentials and require the account owner to complete interactive authentication.

## Report every session

Return a handoff using section D of `../../templates/git-task-template.md`.
Always state whether the result is only prepared, ready to commit, pushed, in review, or merged.
Never describe a prepared change as contributed by a member before that member has actually participated and pushed it.
