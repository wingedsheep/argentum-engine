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

## No subagents — every stage runs inline

Codex has no subagent primitive, so the dispatch list in the `/loop` variant
([How the driving session stays flat](set-implementation-loop.md#how-the-driving-session-stays-flat))
has no equivalent here: the Scryfall payloads, the card scripts, the gate log and the review all land in
the goal's own context. Two consequences worth planning for — the context fills far faster than the Claude
Code variant, and the review is not independent of the author, so read the review comments on a
goal-produced PR with that in mind.

Four of the seven practices in that section survive without delegation, and the objective below leans on
them harder because they're all it has:

- **State on disk.** `.claude/loop-runs/<code>-cards.md` (gitignored) records one line per unit and marks
  blocked cards `needs-feature`. A goal survives compaction; its *context* doesn't, and the ledger is what
  the objective re-reads instead of re-deriving the work list every pass.
- **Trim at the source.** The `--jq` on `gh pr list` reduces each PR to one line and computes the
  set-membership test on the way past, rather than pulling every changed file's diff counts into context.
- **Never `cat` a log.** Tee the gate output to `gate.log` and `grep` the failures out of it. A full
  `just build` log is the single largest thing that can enter an inline run's context, and none of it is
  needed once the failing test names are known.
- **Never retry blind.** One retry, then mark the unit blocked. In an inline run a retry costs context as
  well as a gate slot.

## The command

Substitute the set name for `<SET>`, its lowercase Scryfall code for `<code>`, and the driving model's id
for `<MODEL_ID>` — the Codex TUI status line and `/model` report it, and `~/.codex/config.toml` has a
`model = ` key only when one is pinned.
Why the PRs carry that tag at all is in
[Why the PRs are labelled](set-implementation-loop.md#why-the-prs-are-labelled); it matters more here,
since a goal can run unattended overnight and put a dozen PRs in the list before anyone looks.

The batch policy and the reasoning behind it are in
[Why batches](set-implementation-loop.md#why-batches); why the PR scan is set-scoped is in
[Why the set scope matters](set-implementation-loop.md#why-the-set-scope-matters) — it bites harder
here, since a goal survives compaction and can spend a whole overnight run on the wrong PR.

The worktree rule ([Why a worktree](set-implementation-loop.md#why-a-worktree)) is restated inside
the objective for the same reason: Codex has no `EnterWorktree` to keep the session pinned, so after
a compaction the only things that remember the work isn't happening on `main` are the objective text
and the ledger. Hence the explicit "re-check `git worktree list` and `pwd`" in step 1.

```
/goal Implement every unimplemented card in MTG set <SET>, one reviewed PR at a time, labelling every PR as loop-produced.

These are Claude Code skills, not Codex commands — read the file when the step calls for it:
- .agents/skills/add-card/SKILL.md       implementing a single card
- .agents/skills/add-feature/SKILL.md    any capability that isn't one card
- .agents/skills/review-changes/SKILL.md reviewing a PR
- .agents/skills/verify/SKILL.md         which build/test gate to run

Your context is the scarce resource — you have no subagents to spend it in, so protect it directly: keep the run's state in the ledger rather than in the conversation, reduce every command's output at the source, and never paste a build log, a raw `gh --json` payload, or a card file you have already written.

Repeat until the set is done:
1. Re-orient from real state — read the ledger `.claude/loop-runs/<code>-cards.md` (create it if missing: a `# Loop run: <code>-cards` header, the line `legend: [ ] pending · [~] implementing · [r] in review · [c] correcting · [x] done · [!] needs human · [-] skipped`, and a `## Units` section; it is gitignored, never commit it), then `gh pr list --author @me --state open --json number,title,headRefName,files,reviewDecision,statusCheckRollup --jq '.[] | "#\(.number) \(.headRefName) review=\(.reviewDecision // "none") checks=\([.statusCheckRollup[]?|.conclusion // .state]|unique|join(",")) mine=\([.files[].path]|any(test("definitions/<code>/")))"'` — one line per PR, never the raw JSON — plus `git worktree list`, `pwd`, and the current branch. After a compaction, re-read all of it; never assume where you left off or which tree you are in. A PR belongs to this goal only when `mine=true`, i.e. it ships <SET> content; judge by changed files, not by the title. Open PRs for other sets or other work are outside the goal: leave them alone, do not review, fix, or merge them.
2. No <SET> PR of ours open → work in your own worktree, never in the shared main checkout: reuse the worktree holding this set's in-flight branch if there is one, else `git worktree add .claude/worktrees/<code>-cards -b worktree-<code>-cards main`. Every file path, every `just` gate, every commit and `gh` call runs from that worktree — a command that leaves it gates the wrong tree and reports a green for a build that never saw your cards. Then pick the next unit: run `just card-status --set <code> --list`, skip every card the ledger already marks done or `needs-feature`, and take up to five cards that compose entirely from existing Effects.*/Patterns.* — prefer a shared colour, mechanic, or cycle — following add-card. A card needing a large new engine/SDK capability is NOT batchable: give it its own PR via add-feature. Append the unit line to the ledger before you start. Commit each card separately as you finish it. Then gate once for the whole unit per verify/SKILL.md, teeing the output to gate.log and reading only the failures out of it with `grep`, and open the PR with `gh pr create --title "[agent-loop: <MODEL_ID>] Add <N> <SET> cards"` — that prefix is mandatory on every PR this goal opens, and <MODEL_ID> is the model driving this goal. Say in the body that the PR came from an agentic loop driven by <MODEL_ID>, which gate ran, and what was NOT checked (no manual playthrough, no UX pass, no e2e).
3. <SET> PR open, unreviewed → follow review-changes on it and post the findings as a comment on the PR, so the record lives there rather than in your context. Note only the counts by severity in the ledger.
4. Findings unresolved → fix what holds up, decline what doesn't and say so in a reply on the PR, re-verify, push.
5. Clean and checks green → `gh pr merge --squash --delete-branch` from the main checkout (not from inside the branch's own worktree, or the local branch delete fails), then `git worktree remove` that worktree, back to main, pull. Mark the unit `[x]` in the ledger with the PR number. The next batch branches a fresh worktree from updated main.

Constraints: one batch or one feature per PR, never two <SET> PRs in flight — a PR for some other set does not block you from starting one here. A card that turns out to need new SDK vocabulary drops out of the batch — reset its commit, record it in the ledger as `needs-feature` so no later pass re-picks it, and ship the rest. Build only through `just`, never raw ./gradlew, and gate once per batch rather than per card. Each card still gets its own definition file and its own scenario test file — never a shared batch test. Never revert, stash, or discard changes you did not make; if someone else's work breaks the build, report it and mark the goal blocked. Do not retry a failed step more than once — mark the unit `[!]` in the ledger with the reason and move on; three consecutive failures is environmental, so stop.

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

For what it has actually shipped, read `.claude/loop-runs/<code>-cards.md` — one line per unit, with
PR numbers on the done ones and a reason on anything marked `[!]` or `needs-feature`. That file is
also what lets the run be picked up by a different session, or by the Claude Code variant.
