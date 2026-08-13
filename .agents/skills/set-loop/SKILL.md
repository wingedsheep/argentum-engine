---
name: set-loop
description: Launch a self-continuing agent loop that implements a whole MTG set, one reviewed PR at a time, using the harness's own loop primitive — Claude Code `/loop`, Codex `/goal`, or a plain repeat-until-done session. Composes the loop prompt with the set, the driving model id, and the PR labelling rule. Use when asked to "implement all of set X", "work through set X until it's done", or "keep shipping PRs until the set is finished".
argument-hint: <SET name or code>
---

# Set loop

Ships every unimplemented card in a set, one reviewed PR at a time, by handing the harness a
self-continuing prompt. You **compose and launch** that prompt — you don't implement the set yourself in
this turn.

The same loop body runs under any harness. What differs is only the wrapper that re-fires it and whether
the loop can delegate to subagents.

| Harness | Wrapper | Delegation |
|---|---|---|
| Claude Code | `/loop <prompt>` — self-paced via `ScheduleWakeup` when no interval is given | subagents available: gate and review run in their own context |
| Codex | `/goal <objective>` — a persistent objective row, survives compaction | none: gate and review run inline |
| anything else | paste the body into a session and tell it to repeat until done | none |

Self-paced is the right mode for `/loop` here: a card takes minutes, a feature can take far longer, and a
fixed interval would either thrash or idle. The prompt fires **unchanged** every iteration, so it re-orients
from real state (open PRs, worktrees, current branch) rather than carrying memory between turns.

## Step 1 — resolve the two substitutions

**`<SET>`** — the set the user named, written the way the card list writes it (full name is safest;
`card-status --set <CODE>` takes the code).

**`<MODEL_ID>`** — the model that will drive the loop, since every PR the loop opens is labelled with it.
Resolve it now, in this session, and bake the literal string into the prompt: the loop must not have to
re-derive it later, and a subagent asked "what model are you?" answers for *itself*, which is the wrong
answer for the title.

- **Claude Code** — the session's system prompt names the exact model id; `/status` shows it too. Drop any
  context-window suffix: `claude-opus-5[1m]` → `claude-opus-5`.
- **Codex** — the TUI status line and `/model` report the session model; `~/.codex/config.toml` has a
  `model = ` key only when one is pinned.
- **Anything else** — whatever id the harness reports for this session.

If you genuinely cannot determine it, **ask the user once**. Never guess a version, and never launch the
loop without the tag.

## Step 2 — why the PR label is not optional

Every PR the loop opens carries the run's origin in its **title**:

```
[agent-loop: <MODEL_ID>] <house-style title>
```

```
[agent-loop: claude-opus-5] Add five Lorwyn Eclipsed cards
[agent-loop: gpt-5.6-codex] Add Ponder to Lorwyn
```

Prefix, so it survives truncation in `gh pr list` and stays greppable. `<MODEL_ID>` names the model driving
the loop, not the harness and not the CLI — `claude-opus-5`, not "Claude Code".

This is disclosure, not decoration. `CONTRIBUTING.md` is explicit that agent-generated card batches nobody
read don't get merged, so a human scanning the PR list has to be able to tell at a glance which PRs came out
of an unattended loop, and which model produced them — that is the difference between a PR you skim and one
you read line by line. A loop PR that hides its origin is a defect even when the code is right.

## Step 3 — launch

Substitute `<SET>` and `<MODEL_ID>` throughout, then send the wrapper for the current harness. Claude Code:

```
/loop Ship cards for MTG set <SET> one PR at a time, and label every PR as loop-produced. Orient first — run `gh pr list --author @me --state open --json number,title,headRefName,files,reviewDecision,statusCheckRollup`, `git worktree list`, and check the current branch. A PR counts for this loop only if it ships <SET> content — judge that by its changed files (definitions under the set's package, that set's snapshot), not by its title alone. Open PRs for other sets or other work are NOT yours to advance: leave them alone, don't review, fix, or merge them.

Do the work in your own worktree, never in the shared main checkout: if a worktree already holds this set's in-flight branch, work there; otherwise `git worktree add .claude/worktrees/<set>-cards -b worktree-<set>-cards main` before touching a file. Every Read/Edit/Write path, every `just` gate, every commit and `gh` call runs from that worktree path — a stray `cd` back to the repo root edits the wrong tree and reports a green from a build that never saw your cards. Then advance exactly ONE step:

1. No open <SET> PR of ours → pick the next unimplemented cards from <SET> (compare the set's card list against `mtg-sets` definitions). Group up to five that all compose from existing Effects.*/Patterns.* into one batch and implement them with /add-card, preferring cards sharing a colour, mechanic, or cycle. A card that needs a large new engine/SDK capability is NOT batchable — give it its own PR via /add-feature. Gate once for the whole batch, then open the PR with `gh pr create --title "[agent-loop: <MODEL_ID>] Add <N> <SET> cards"` — that prefix is mandatory on every PR this loop opens, and <MODEL_ID> is the model driving this loop, not a subagent's own model. Say in the body that the PR came from an agentic loop driven by <MODEL_ID>, which gate ran, and what was NOT checked (no manual playthrough, no UX pass, no e2e).
2. Open <SET> PR with no review yet → review it per /review-changes and post the findings on the PR.
3. Open <SET> PR with unresolved review findings → fix them, re-gate, push.
4. Open <SET> PR clean and checks green → `gh pr merge --squash --delete-branch` (run it from the main checkout, not from inside the branch's own worktree, or deleting the local branch fails), then `git worktree remove` that worktree, switch to main and pull — the next batch branches a fresh one from updated main.

Delegate the two context-heavy steps to subagents so this session stays lean over a long run: run the gate in a subagent (pass it the worktree path, have it run the right `just` gate and return only pass/fail plus any failing test names — not the build log), and run step 2's review in a fresh subagent that did not write the code (pass it the worktree path and PR number, have it post its own findings comment and return only the counts by severity). Implement the cards yourself; that's the part where the context is the work.

Rules: one batch or one feature per PR, never two <SET> PRs in flight — a PR for some other set does not block you from starting one here. A card that turns out to need new SDK vocabulary drops out of the batch — reset its commit, note it, ship the rest. Run gates through `just`, never raw ./gradlew, and gate once per batch rather than per card. Each card still gets its own definition file and its own scenario test file — never a shared batch test. Never revert or stash changes you didn't make; if someone else's work breaks the build, report it and stop. When every card in <SET> is implemented, say so and stop the loop.
```

**Codex** takes the same body under `/goal`, with three changes — the full text is in
[`docs/agent-loops/set-implementation-goal.md`](../../../docs/agent-loops/set-implementation-goal.md):

- Prepend the skill file paths (`.agents/skills/add-card/SKILL.md`, …). Codex only discovers skills in
  `.codex/skills`, so `/add-card` and friends are not commands there — they're files to read. Fixable once
  with `mkdir -p .codex && ln -s ../.agents/skills .codex/skills`.
- Drop the delegation paragraph; there are no subagents. Gate and review run inline.
- Add "re-check `git worktree list` and `pwd`" to step 1 and end with a completion criterion rather than a
  stop instruction — a goal survives compaction, and after one the objective text is the only thing that
  remembers the work isn't happening on `main`.

Then tell the user the loop is running, which set, and the label its PRs will carry.

## Why it's shaped this way

- **[Why batches](../../../docs/agent-loops/set-implementation-loop.md#why-batches)** — `CONTRIBUTING.md`
  lets cards built entirely from existing primitives share a PR; one 30-minute gate slot then covers five
  cards. The rule that does *not* relax: one definition file and one `{CardName}ScenarioTest.kt` per card.
- **[Why the set scope matters](../../../docs/agent-loops/set-implementation-loop.md#why-the-set-scope-matters)**
  — `--author @me` returns every PR you have open. An unscoped "no open PR of ours" makes the loop spend its
  iterations advancing someone else's unit while its own set never starts.
- **[Why a worktree](../../../docs/agent-loops/set-implementation-loop.md#why-a-worktree)** — the main
  checkout is shared ground, and a `just` gate run from the wrong tree reports BUILD SUCCESSFUL for a build
  that never saw your cards.

## Caveats to pass on

- **Step 1 re-derives the work list every iteration.** The tail of a set is where the hard cards cluster, so
  the loop can keep re-picking whatever it judges smallest and stall on a genuinely blocked card. If the set
  has a backlog file, point step 1 at it so progress is recorded on disk instead of re-inferred.
- **Context still grows in the driving session** even with the gate and review delegated — implementation
  runs there. A 200-card set will need more than one session; the loop resumes cleanly because every
  iteration re-orients from open PRs and worktrees.
- **Cards are not played.** No manual playthrough, no UX pass from both seats, no e2e. Each PR body says so,
  and that pass is still the user's.
