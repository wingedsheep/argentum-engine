# Set implementation goal (Codex `/goal`)

The [Claude Code `/loop`](set-implementation-loop.md) workflow, ported to Codex. Both are launched by
the [`set-loop`](../../.agents/skills/set-loop/SKILL.md) skill, which picks the wrapper for whichever
harness is running it and substitutes the driving model id.

`/goal` is a **builtin** in Codex (verified on 0.147.0) — there is no custom command to author.
The `goals` feature is stable and enabled by default, and `~/.codex/goals_1.sqlite` holds one
`thread_goals` row per thread: an `objective`, a status (`active` / `paused` / `blocked` /
`usage_limited` / `budget_limited` / `complete`), and an optional token budget.

That makes it a persistent, self-continuing objective rather than a prompt re-fired into fresh
context — so the text below reads as a goal with a completion criterion, not as a per-iteration
state machine.

## Skills are not discoverable by Codex

Codex only looks in `.codex/skills` and `~/.codex/skills`. This repo's skills live in
`.agents/skills/` (`.claude/skills` is a symlink to it), so `/add-card`, `/review-changes` and
friends **do not exist as commands in Codex** and must be loaded by path.

To fix that properly, mirror the Claude symlink:

```bash
mkdir -p .codex && ln -s ../.agents/skills .codex/skills
```

Codex then discovers them as real skills and the file-path list can be dropped from the
objective.

`AGENTS.md` at the repo root *is* read natively by Codex, so the project hard rules are already
in context. The few restated in the objective are there because they survive compaction better
than the file does, and they are the ones that break an unattended overnight run.

## No subagents — gate and review run inline

Codex has no subagent primitive, so the delegation paragraph in the `/loop` variant
([Why delegate the gate and the review](set-implementation-loop.md#why-delegate-the-gate-and-the-review))
has no equivalent here: the gate log and the review both land in the goal's own context. Two
consequences worth planning for — the context fills faster than the Claude Code variant, and the review
is not independent of the author, so read the review comments on a goal-produced PR with that in mind.

## The command

Substitute the set name for `<SET>` and the driving model's id for `<MODEL_ID>` — the Codex TUI status
line and `/model` report it, and `~/.codex/config.toml` has a `model = ` key only when one is pinned.
Why the PRs carry that tag at all is in
[Why the PRs are labelled](set-implementation-loop.md#why-the-prs-are-labelled); it matters more here,
since a goal can run unattended overnight and put a dozen PRs in the list before anyone looks.

The batch policy and the reasoning behind it are in
[Why batches](set-implementation-loop.md#why-batches); why the PR scan is set-scoped is in
[Why the set scope matters](set-implementation-loop.md#why-the-set-scope-matters) — it bites harder
here, since a goal survives compaction and can spend a whole overnight run on the wrong PR.

The worktree rule ([Why a worktree](set-implementation-loop.md#why-a-worktree)) is restated inside
the objective for the same reason: Codex has no `EnterWorktree` to keep the session pinned, so after
a compaction the only thing that remembers the work isn't happening on `main` is the objective text.
Hence the explicit "re-check `git worktree list` and `pwd`" in step 1.

```
/goal Implement every unimplemented card in MTG set <SET>, one reviewed PR at a time, labelling every PR as loop-produced.

These are Claude Code skills, not Codex commands — read the file when the step calls for it:
- .agents/skills/add-card/SKILL.md       implementing a single card
- .agents/skills/add-feature/SKILL.md    any capability that isn't one card
- .agents/skills/review-changes/SKILL.md reviewing a PR
- .agents/skills/verify/SKILL.md         which build/test gate to run

Repeat until the set is done:
1. Re-orient from real state — `gh pr list --author @me --state open --json number,title,headRefName,files,reviewDecision,statusCheckRollup`, `git worktree list`, `pwd`, and the current branch. After a compaction, re-read; never assume where you left off or which tree you are in. A PR belongs to this goal only if it ships <SET> content — judge that by its changed files (definitions under the set's package, that set's snapshot), not by its title alone. Open PRs for other sets or other work are outside the goal: leave them alone, do not review, fix, or merge them.
2. No <SET> PR of ours open → work in your own worktree, never in the shared main checkout: reuse the worktree holding this set's in-flight branch if there is one, else `git worktree add .claude/worktrees/<set>-cards -b worktree-<set>-cards main`. Every file path, every `just` gate, every commit and `gh` call runs from that worktree — a command that leaves it gates the wrong tree and reports a green for a build that never saw your cards. Then pick the next unimplemented cards from <SET> (diff the set's card list against the definitions under mtg-sets). Group up to five that all compose from existing Effects.*/Patterns.* into one batch and follow add-card, preferring cards sharing a colour, mechanic, or cycle. A card needing a large new engine/SDK capability is NOT batchable — give it its own PR via add-feature. Verify once for the batch per verify/SKILL.md, then open the PR with `gh pr create --title "[agent-loop: <MODEL_ID>] Add <N> <SET> cards"` — that prefix is mandatory on every PR this goal opens, and <MODEL_ID> is the model driving this goal. Say in the body that the PR came from an agentic loop driven by <MODEL_ID>, which gate ran, and what was NOT checked (no manual playthrough, no UX pass, no e2e).
3. <SET> PR open, unreviewed → follow review-changes on it.
4. Findings unresolved → fix, re-verify, push.
5. Clean and checks green → `gh pr merge --squash --delete-branch` from the main checkout (not from inside the branch's own worktree, or the local branch delete fails), then `git worktree remove` that worktree, back to main, pull. The next batch branches a fresh worktree from updated main.

Constraints: one batch or one feature per PR, never two <SET> PRs in flight — a PR for some other set does not block you from starting one here. A card that turns out to need new SDK vocabulary drops out of the batch — reset its commit, note it, ship the rest. Build only through `just`, never raw ./gradlew, and gate once per batch rather than per card. Each card still gets its own definition file and its own scenario test file — never a shared batch test. Never revert, stash, or discard changes you did not make; if someone else's work breaks the build, report it and mark the goal blocked.

Done when every card in <SET> is implemented and merged. If a card turns on a rules question you can't confirm against the Comprehensive Rules, mark blocked rather than guessing.
```

## Checking on a long run

Without attaching to the TUI:

```bash
sqlite3 -header ~/.codex/goals_1.sqlite \
  "select status, tokens_used, time_used_seconds from thread_goals order by updated_at_ms desc limit 1;"
```

A run that stopped on its own will show `blocked`, `usage_limited`, `budget_limited`, or
`complete` rather than `active`.
