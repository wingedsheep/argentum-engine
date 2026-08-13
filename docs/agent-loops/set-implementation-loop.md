# Set implementation loop (Claude Code `/loop`)

Ships every unimplemented card in a set, one reviewed PR at a time, in a Claude Code session.

The [`set-loop`](../../.agents/skills/set-loop/SKILL.md) skill composes and launches this for you —
`/set-loop <SET>` resolves the driving model id and substitutes it into the prompt. This page is the
reference for the prompt itself, and for the reasoning behind its shape.

`/loop [interval] <prompt>` re-fires the prompt on a schedule; omit the interval and the model
self-paces via `ScheduleWakeup`. Self-paced is the right mode here — a card takes minutes, a
feature can take far longer, and a fixed interval would either thrash or idle.

The prompt fires **unchanged** every iteration, so it must re-orient from real state (open PRs,
current branch) rather than carry memory between turns.

Every PR it opens is titled `[agent-loop: <MODEL_ID>] …` — see
[Why the PRs are labelled](#why-the-prs-are-labelled).

"Ours" means **this set's** PRs, not every PR you happen to have open. Scope it that way or the
loop adopts unrelated in-flight work — see [Why the set scope matters](#why-the-set-scope-matters).

The loop also implements in **its own worktree**, never in the shared main checkout — see
[Why a worktree](#why-a-worktree).

## The command

Substitute the set name for `<SET>` and the driving model's id for `<MODEL_ID>` (Claude Code names it
in the session's system prompt and in `/status`; drop the context-window suffix, so
`claude-opus-5[1m]` → `claude-opus-5`).

```
/loop Ship cards for MTG set <SET> one PR at a time, and label every PR as loop-produced. Orient first — run `gh pr list --author @me --state open --json number,title,headRefName,files,reviewDecision,statusCheckRollup`, `git worktree list`, and check the current branch. A PR counts for this loop only if it ships <SET> content — judge that by its changed files (definitions under the set's package, that set's snapshot), not by its title alone. Open PRs for other sets or other work are NOT yours to advance: leave them alone, don't review, fix, or merge them.

Do the work in your own worktree, never in the shared main checkout: if a worktree already holds this set's in-flight branch, work there; otherwise `git worktree add .claude/worktrees/<set>-cards -b worktree-<set>-cards main` before touching a file. Every Read/Edit/Write path, every `just` gate, every commit and `gh` call runs from that worktree path — a stray `cd` back to the repo root edits the wrong tree and reports a green from a build that never saw your cards. Then advance exactly ONE step:

1. No open <SET> PR of ours → pick the next unimplemented cards from <SET> (compare the set's card list against `mtg-sets` definitions). Group up to five that all compose from existing Effects.*/Patterns.* into one batch and implement them with /add-card, preferring cards sharing a colour, mechanic, or cycle. A card that needs a large new engine/SDK capability is NOT batchable — give it its own PR via /add-feature. Verify once for the batch, then open the PR with `gh pr create --title "[agent-loop: <MODEL_ID>] Add <N> <SET> cards"` — that prefix is mandatory on every PR this loop opens, and <MODEL_ID> is the model driving this loop, not a subagent's own model. Say in the body that the PR came from an agentic loop driven by <MODEL_ID>, which gate ran, and what was NOT checked (no manual playthrough, no UX pass, no e2e).
2. Open <SET> PR with no review yet → run /review-changes on it.
3. Open <SET> PR with unresolved review findings → fix them, re-verify, push.
4. Open <SET> PR clean and checks green → `gh pr merge --squash --delete-branch` (run it from the main checkout, not from inside the branch's own worktree, or deleting the local branch fails), then `git worktree remove` that worktree, switch to main and pull — the next batch branches a fresh one from updated main.

Delegate the two context-heavy steps to subagents so this session stays lean over a long run: run the gate in a subagent (pass it the worktree path, have it run the right `just` gate and return only pass/fail plus any failing test names — not the build log), and run step 2's review in a fresh subagent that did not write the code (pass it the worktree path and PR number, have it post its own findings comment and return only the counts by severity). Implement the cards yourself; that's the part where the context is the work.

Rules: one batch or one feature per PR, never two <SET> PRs in flight — a PR for some other set does not block you from starting one here. A card that turns out to need new SDK vocabulary drops out of the batch — reset its commit, note it, ship the rest. Run gates through `just`, never raw ./gradlew, and gate once per batch rather than per card. Each card still gets its own definition file and its own scenario test file — never a shared batch test. Never revert or stash changes you didn't make; if someone else's work breaks the build, report it and stop. When every card in <SET> is implemented, say so and stop the loop.
```

## Why the PRs are labelled

```
[agent-loop: claude-opus-5] Add five Lorwyn Eclipsed cards
```

`CONTRIBUTING.md` is explicit that agent-generated card batches nobody read don't get merged. A loop
run can put a dozen PRs in the list overnight, and from the outside they look exactly like
hand-written ones — so the title has to say both that a loop produced it and which model was driving,
because that's the difference between a PR you skim and one you read line by line.

Prefix rather than suffix: it survives truncation in `gh pr list` and in GitHub's list view, and it
makes the whole run greppable afterwards. The id names the *model*, not the harness — `claude-opus-5`,
not "Claude Code" — and not a subagent's own model, since the gate and review subagents may be running
on something else entirely.

## Why delegate the gate and the review

A `just build` log and a full review pass are the two biggest context sinks in an iteration, and
neither produces anything the loop needs to keep. Pushed into subagents they come back as a pass/fail
line and a set of finding counts, which is all step 3 acts on.

The review gains something beyond context economy: a subagent that did not write the cards reads the
diff without the author's context, which is where "the code does what I meant" bugs actually surface.
Implementation stays in the driving session — that's the step where holding the context *is* the work,
and handing a card batch to a subagent means re-establishing everything it needs to know.

Subagents don't buy parallelism here. Gradle is serialised by a machine-global lock, so a delegated
gate blocks exactly as long as an inline one; what changes is only what comes back.

## Why batches

`CONTRIBUTING.md` sets the shape: cards built entirely from existing `Effects.*` / `Patterns.*` may share
a PR, while a card introducing new engine vocabulary gets one to itself with tests for the primitive. For
the loop that also means one 30-minute gate slot covers five cards instead of one, which is most of the
wall-clock cost of a run.

The rule that does **not** relax: each card keeps its own definition file and its own
`{CardName}ScenarioTest.kt`. Batching is a PR-shape decision and never merges two cards into one artifact
(AGENTS.md → Hard rules).

## Why the set scope matters

`--author @me` returns every PR you have open, and a repo this size usually has one — an earlier
loop's leftover, a worktree agent's card batch, a half-finished feature. An unscoped "no open PR of
ours" reads that as step 2/3/4 work and the loop spends its iterations reviewing and merging someone
else's unit while its own set never starts. Worse, "never two in flight" then blocks the set
indefinitely on a PR the loop didn't open and may not be able to finish.

Scope by changed files rather than title. Titles drift ("Add Riddles in the Dark to The Hobbit"
names its set; plenty don't), while a set PR always touches
`mtg-sets/.../definitions/{set}/cards/` and that set's snapshot under
`mtg-sets/src/test/resources/snapshots/cards/{SET}.json`.

This also makes it safe to run one loop per set concurrently: each only ever advances its own PRs.

## Why a worktree

The main checkout is shared ground — other agents, other loops, and the user's own editor all live
in it. A loop that implements there leaves uncommitted card files in a tree someone else is about to
commit, check out, or reset, and AGENTS.md's "never revert changes you didn't make" then cuts both
ways: your work is indistinguishable from theirs, so neither side can clean up safely.

One worktree per PR keeps the unit self-contained — branch, uncommitted state, and the gate run that
proves it all live in one directory that gets thrown away after the merge. It is also what makes the
concurrency in [Why the set scope matters](#why-the-set-scope-matters) real: separate PRs is only
half of it if two loops still share one working tree.

Three things that have actually gone wrong:

- **Paths, not `cd`.** Once you're in a worktree, every file path and every Bash command must be
  worktree-rooted. A compound command that `cd`s elsewhere resets the shell back afterwards, and a
  `just` gate run from the repo root does not fail with "no tests found" — it reports BUILD
  SUCCESSFUL for tests that never included your card. Check the Gradle problems-report path points
  into the worktree before believing a green.
- **`EnterWorktree` isolates the whole session.** From inside one, writes to a sibling worktree or to
  the main checkout are hard-blocked. That suits a loop owning one unit at a time; if the session is
  *already* isolated, branch inside it (`git checkout -b … main`) rather than adding a sibling you
  can create but never write to.
- **Worktrees don't buy parallel builds.** Gradle is serialised by a machine-global lock for good
  reason — three concurrent worktree builds have OOM'd the Kotlin daemon here. The worktree isolates
  *files*, not the build.

## Caveats

**Context still grows in your session.** Delegating the gate and the review keeps the two largest
sinks out of it, but implementation runs in the driving session, so a 200-card set will need more
than one. That's survivable rather than fatal: every iteration re-orients from open PRs and
worktrees, so a fresh session picks the loop up where the last one left it.

**Step 1 re-derives the work list every iteration.** The tail of a set is where the hard cards
cluster, so the loop can keep re-picking whatever it judges smallest and stall on a genuinely
blocked card. If the set has a backlog file, point step 1 at it so progress is recorded on disk
instead of re-inferred.

## See also

- [Codex `/goal` variant](set-implementation-goal.md) — same workflow, different harness
