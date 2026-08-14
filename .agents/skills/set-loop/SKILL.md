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
| Claude Code | `/loop <prompt>` — self-paced via `ScheduleWakeup` when no interval is given | subagents available: picking, implementation, gate, review and fixes all run in their own context |
| Codex | `/goal <objective>` — a persistent objective row, survives compaction | none: every stage runs inline |
| anything else | paste the body into a session and tell it to repeat until done | none |

Self-paced is the right mode for `/loop` here: a card takes minutes, a feature can take far longer, and a
fixed interval would either thrash or idle. The prompt fires **unchanged** every iteration **into the same
session**, so it re-orients from real state (the ledger, open PRs, worktrees) rather than carrying memory
between turns — and every iteration must cost the driving session almost nothing, because nothing resets it.
That is why the body reads as an orchestrator's dispatch list: see
[How the driving session stays flat](../../../docs/agent-loops/set-implementation-loop.md#how-the-driving-session-stays-flat).

## Step 1 — resolve the substitutions

**`<SET>`** — the set the user named, written the way the card list writes it (full name is safest).

**`<code>`** — its lowercase Scryfall code (`hob`, `blb`). One substitution serves both the definitions path
and `just card-status --set`, which upper-cases the argument itself.

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

Substitute `<SET>`, `<code>` and `<MODEL_ID>` throughout, then send the wrapper for the current harness.
Claude Code:

```
/loop Ship cards for MTG set <SET> one PR at a time, and label every PR as loop-produced.

You are the ORCHESTRATOR. You dispatch, you decide, you record. You do NOT read card files, diffs, build logs, `card-status` output, or review bodies — a subagent reads those and hands you back a short verdict block. A 100-card run must cost your context roughly what a 5-card run costs; if you catch yourself about to open a `.kt` file or a log, that is a subagent's job.

ORIENT — cheaply, every iteration. The ledger is your memory between turns; this conversation is not.
- Read `.claude/loop-runs/<code>-cards.md`. If it doesn't exist, create it with a `# Loop run: <code>-cards` header, the line `legend: [ ] pending · [~] implementing · [r] in review · [c] correcting · [x] done · [!] needs human · [-] skipped`, and a `## Units` section. It is gitignored — never commit it.
- `gh pr list --author @me --state open --json number,title,headRefName,files,reviewDecision,statusCheckRollup --jq '.[] | "#\(.number) \(.headRefName) review=\(.reviewDecision // "none") checks=\([.statusCheckRollup[]?|.conclusion // .state]|unique|join(",")) mine=\([.files[].path]|any(test("definitions/<code>/")))"'` — one line per PR, never the raw JSON. A PR is yours only when `mine=true`, i.e. it ships <SET> content; judge by changed files, not by the title. Other people's PRs are NOT yours to advance: don't review, fix, or merge them.
- `git worktree list`, and the current branch.

WORKTREE — do the work in your own, never in the shared main checkout. If one already holds this set's in-flight branch, use it; otherwise `git worktree add .claude/worktrees/<code>-cards -b worktree-<code>-cards main` before touching a file. Pass that absolute path to every subagent, and run every path, `just` gate, commit and `gh` call from it — a stray `cd` back to the repo root gates a tree that never saw your cards and reports a green.

Then advance exactly ONE step:

1. No <SET> PR of ours open → build the next unit.
   a. PICK — one subagent: "In <worktree>, run `just card-status --set <code> --list`, read `.claude/loop-runs/<code>-cards.md`, and skip every card already done or marked needs-feature there. Choose the next unit: up to five cards that compose entirely from existing Effects.*/Patterns.* — prefer a shared colour, mechanic, or cycle — or a single card that needs new engine/SDK vocabulary, on its own. Append the unit line to the ledger. Return only: UNIT / KIND (batch|solo-feature|none-left) / CARDS (semicolon-separated) / REASON (one line)."
   b. IMPLEMENT — one subagent per card, dispatched together: "In <worktree>, implement <Card> for <SET> following /add-card Steps 0-9. Write files only: do NOT run git, `just`, or any build, and do not touch the backlog. If Step 4 says the card needs new SDK vocabulary, delete what you wrote and report it dropped. Return only: CARD / STATUS (written|dropped|failed) / FILES (paths) / NOTE (one line — which primitives it composes, or why dropped)." A solo-feature unit is instead one subagent following /add-feature.
   c. Commit each written card yourself, from the FILES paths you were given: `git -C <worktree> add <paths> && git -C <worktree> commit -m "Add <Card> to <Set>"`. One commit per card, so a bad card can be dropped without unpicking the others. Never `git add -A` — a sibling agent's in-flight file is not yours to commit.
   d. GATE — one subagent: "In <worktree>, tick these cards in the set's `backlog/sets/*/cards.md` if one lists them and run `just fix-backlog`, run `just check-card-printing` for each card, then run the right gate per /verify (`just build` for cards on existing primitives, `just test` when new engine behaviour landed). Expect a CardDefinitionSnapshotTest diff: re-bless with `just rebless-cards` and confirm ONLY these cards moved in the golden — an unrelated card moving means shared SDK behaviour changed, so stop and report it rather than re-blessing past it. Tee the build output to <worktree>/gate.log. Return only: GATE (command) / STATUS (passed|failed) / FAILING (test names, or -) / MOVED (unexpected snapshot entries, or -)."
   e. Green → commit the bookkeeping, push, and open the PR with `gh pr create --title "[agent-loop: <MODEL_ID>] Add <N> <SET> cards"`. That prefix is mandatory on every PR this loop opens, and <MODEL_ID> is the model driving this loop, not a subagent's own model. Write the body from the verdict blocks you already hold — one line per card, plus any card dropped and why — and say that the PR came from an agentic loop driven by <MODEL_ID>, which gate ran, and what was NOT checked (no manual playthrough, no UX pass, no e2e). Red → dispatch ONE fix subagent with the FAILING names and the log path; if it is still red after that, mark the unit `[!]` in the ledger and stop for a human.
2. Our <SET> PR is open and unreviewed → REVIEW — a fresh subagent that did not write the code: "In <worktree>, run /review-changes on PR #<N> and post your findings as a comment on that PR. Change no code. Return only: PR / SCOPE (full|cards|skipped) / FINDINGS (N blocking, N important, N minor) / NOTE (one line, only if something is blocking)." Record the counts in the ledger. Do not read the comment.
3. Our <SET> PR has unresolved findings → FIX — one subagent: "In <worktree>, read the review comment on PR #<N> via `gh pr view <N> --comments`, fix what holds up, decline what doesn't and say so in a reply on the PR, re-run the gate per /verify, and push to the same branch. Return only: PR / FIXED (N of N, N declined) / GATE (passed|failed) / STATUS (pushed|needs-human)."
4. Our <SET> PR is clean and its checks are green → `gh pr merge --squash --delete-branch` (run it from the main checkout, not from inside the branch's own worktree, or deleting the local branch fails), then `git worktree remove` that worktree, switch to main and pull. Mark the unit `[x]` in the ledger with the PR number. The next unit branches a fresh worktree from updated main.

VERDICT DISCIPLINE — every dispatch above names exactly what comes back, and that is all you keep. If a subagent returns prose instead, take the first status word you can find and move on; never read its transcript to reconstruct what happened. Findings live on the PR, logs live in the worktree, the work list lives in the ledger — none of them belong in your context.

RULES: one unit per PR, and never two <SET> PRs in flight — a PR for another set does not block you from starting one here. Each card keeps its own definition file and its own scenario test file; never a shared batch test. Gates run through `just`, never raw ./gradlew, and once per unit, never per card. Never revert or stash changes you didn't make; if someone else's work breaks the build, report it and stop. Don't retry a failed subagent more than once — mark `[!]` with the reason and move on; three consecutive failures is environmental, so stop and report. When `card-status` shows <SET> complete, say so and stop the loop.
```

**Codex** takes the same body under `/goal`, with three changes — the full text is in
[`docs/agent-loops/set-implementation-goal.md`](../../../docs/agent-loops/set-implementation-goal.md):

- Prepend the skill file paths (`.agents/skills/add-card/SKILL.md`, …). Codex only discovers skills in
  `.codex/skills`, so `/add-card` and friends are not commands there — they're files to read. Fixable once
  with `mkdir -p .codex && ln -s ../.agents/skills .codex/skills`.
- Collapse the dispatch list into inline steps; there are no subagents, so picking, implementing, gating,
  reviewing and fixing all land in the goal's own context. The ledger, the trimmed `--jq` orientation and
  the never-`cat`-a-log rule carry over unchanged — they're the practices that still work without
  delegation.
- Add "re-check `git worktree list` and `pwd`" to step 1 and end with a completion criterion rather than a
  stop instruction — a goal survives compaction, and after one the objective text and the ledger are the
  only things that remember the work isn't happening on `main`.

Then tell the user the loop is running, which set, and the label its PRs will carry.

## Why it's shaped this way

- **[How the driving session stays flat](../../../docs/agent-loops/set-implementation-loop.md#how-the-driving-session-stays-flat)**
  — `/loop` re-fires into the *same* session, so nothing resets its context. The body is an orchestrator's
  dispatch list with a bounded return contract per stage, a ledger on disk for memory, and a hard ban on
  reading card files, diffs, and logs.
- **[Why implementation is delegated too](../../../docs/agent-loops/set-implementation-loop.md#why-implementation-is-delegated-too)**
  — the context a card needs is `add-card` plus its Scryfall data, not the conversation, so a card agent
  re-establishes it for a fixed cost while an inlining orchestrator pays for the output forever. Card agents
  write files; the orchestrator commits by explicit path; the gate agent does the bookkeeping.
- **[Why batches](../../../docs/agent-loops/set-implementation-loop.md#why-batches)** — `CONTRIBUTING.md`
  lets cards built entirely from existing primitives share a PR; one gate slot then covers five cards. The
  rule that does *not* relax: one definition file and one `{CardName}ScenarioTest.kt` per card.
- **[Why the set scope matters](../../../docs/agent-loops/set-implementation-loop.md#why-the-set-scope-matters)**
  — `--author @me` returns every PR you have open. An unscoped "no open PR of ours" makes the loop spend its
  iterations advancing someone else's unit while its own set never starts.
- **[Why a worktree](../../../docs/agent-loops/set-implementation-loop.md#why-a-worktree)** — the main
  checkout is shared ground, and a `just` gate run from the wrong tree reports BUILD SUCCESSFUL for a build
  that never saw your cards. Fan out card agents freely; never fan out gates.

## Caveats to pass on

- **The orchestrator never sees the code.** The mandatory review subagent — a fresh context that did not
  write the cards — and the gate are the whole quality story for an unattended run. That's what the
  `[agent-loop:]` title is telling a human to account for.
- **Progress lives in `.claude/loop-runs/<code>-cards.md`**, gitignored. It's how a compacted or restarted
  session resumes, and how a card marked `needs-feature` stops being re-picked at the tail of a set. Point
  the user at it if they want to check on a run without attaching.
- **Cards are not played.** No manual playthrough, no UX pass from both seats, no e2e. Each PR body says so,
  and that pass is still the user's.
