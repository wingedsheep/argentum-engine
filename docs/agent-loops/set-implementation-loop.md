# Set implementation loop (Claude Code `/loop`)

Ships every unimplemented card in a set, one reviewed PR at a time, in a Claude Code session.

The [`set-loop`](../../.agents/skills/set-loop/SKILL.md) skill composes and launches this for you —
`/set-loop <SET>` resolves the driving model id and substitutes it into the prompt. This page is the
reference for the prompt itself, and for the reasoning behind its shape.

`/loop [interval] <prompt>` re-fires the prompt on a schedule; omit the interval and the model
self-paces via `ScheduleWakeup`. Self-paced is the right mode here — a card takes minutes, a
feature can take far longer, and a fixed interval would either thrash or idle.

The prompt fires **unchanged** every iteration into the **same session**, so two properties matter
more than anything else in its design: it must re-orient from real state rather than carry memory
between turns, and every iteration must add as little as possible to a context that never resets on
its own. The driving session is therefore an *orchestrator* — it dispatches, decides, and records,
and never reads a card file, a diff, a build log, or a review body. See
[How the driving session stays flat](#how-the-driving-session-stays-flat).

Every PR it opens is titled `[agent-loop: <MODEL_ID>] …` — see
[Why the PRs are labelled](#why-the-prs-are-labelled).

"Ours" means **this set's** PRs, not every PR you happen to have open. Scope it that way or the
loop adopts unrelated in-flight work — see [Why the set scope matters](#why-the-set-scope-matters).

The loop also implements in **its own worktree**, never in the shared main checkout — see
[Why a worktree](#why-a-worktree).

## The command

Substitute the set name for `<SET>`, its lowercase Scryfall code for `<code>` (`card-status`
upper-cases it itself, so one substitution serves both the path and the flag), and the driving
model's id for `<MODEL_ID>` (Claude Code names it in the session's system prompt and in `/status`;
drop the context-window suffix, so `claude-opus-5[1m]` → `claude-opus-5`).

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

## How the driving session stays flat

`/loop` re-fires the prompt into the **same** session. Nothing about the loop primitive resets
context between iterations, so whatever an iteration reads is still there twenty iterations later —
paid for again on every turn until compaction throws it away, and compaction is a loss of *state*,
not a saving. A set is 200+ cards. The only way that run finishes is if an iteration's cost is
roughly constant.

Seven practices get there. They are not specific to this loop; they are what any long-running
orchestration needs.

1. **Orchestrate, don't do.** The driving session decides *what happens next* and records that it
   happened. Everything that requires reading — Scryfall payloads, the SDK reference, card `.kt`
   files, diffs, build logs, review findings — happens in a subagent whose context is discarded when
   it returns. The rule is stated as a prohibition in the prompt on purpose: "delegate the heavy
   steps" is advice a model talks itself out of on a card that looks easy, and one inlined card
   pulls in a Scryfall lookup, the card DSL, and a test file that every later iteration then carries.
2. **Bound every return.** A subagent that reports freely returns a summary as long as the thing it
   summarised. Each dispatch in the prompt ends with "Return only:" and an explicit field list, so
   what crosses back is a handful of lines with a known shape. The fields are chosen to be exactly
   what the next step acts on — `FILES` because the orchestrator commits from it, `FINDINGS` counts
   because step 3 branches on them, and nothing else.
3. **Keep state on disk.** `.claude/loop-runs/<code>-cards.md` (gitignored) is the run's memory: one
   line per unit, a status character, and a note. Re-reading it costs a few hundred tokens and it
   survives compaction, session death, and a machine reboot — which is what makes "a fresh session
   picks the loop up where the last one left it" true rather than aspirational. It also fixes the
   old failure where the tail of a set kept getting re-picked: a card marked `needs-feature` stays
   marked, so iteration 40 doesn't rediscover the same blocked card iteration 12 rejected.
4. **Trim at the source.** `gh pr list --json …` returns every changed file with its diff counts —
   several hundred tokens per PR, of which the loop uses one boolean. The `--jq` in the prompt
   reduces each PR to a single line and computes the set-membership test on the way past. Same idea
   everywhere: `card-status --list` rather than a directory scan, `gh pr view --comments` inside the
   fixer rather than in the orchestrator, and never `cat` a build log.
5. **Let artifacts live where they belong.** Review findings go on the PR, where a human reading it
   later will see them; the build log is a file in the worktree; the work list is the ledger. Each is
   reachable by the agent that needs it and invisible to the one that doesn't. This is why the
   reviewer posts its own comment and the fixer reads it back off GitHub instead of the orchestrator
   relaying it — relaying the findings would put the whole review in the one context that must not
   hold it.
6. **One decision per iteration.** Each turn advances exactly one step and stops. This isn't a
   context saving in itself — it bounds *blast radius*, and it means the state the next iteration
   re-orients from is always real (open PRs, worktrees, ledger) rather than remembered.
7. **Never retry blind.** A failed subagent gets one retry at most, then a `[!]` and a note. Retrying
   a failure you cannot see costs a gate slot and grows context with no new information; three in a
   row means the environment is broken, and every further dispatch will fail the same way.

### What crosses back

The complete set of what an iteration adds to the driving session's context:

| Dispatch | Returns | Why the orchestrator needs it |
|---|---|---|
| PICK | `UNIT` / `KIND` / `CARDS` / `REASON` | which cards to fan out, and whether it's a batch or a solo feature |
| IMPLEMENT (×N) | `CARD` / `STATUS` / `FILES` / `NOTE` | paths to commit, and one PR-body line per card |
| GATE | `GATE` / `STATUS` / `FAILING` / `MOVED` | whether to open the PR or dispatch a fixer |
| REVIEW | `PR` / `SCOPE` / `FINDINGS` / `NOTE` | whether step 3 runs |
| FIX | `PR` / `FIXED` / `GATE` / `STATUS` | whether the unit is mergeable or needs a human |

Roughly thirty lines for a five-card unit. The unit itself — five Scryfall fetches, five card
scripts, a build log, a full review — runs to tens of thousands of tokens that the driving session
never sees.

## Why implementation is delegated too

An earlier version of this loop kept implementation in the driving session, on the reasoning that
holding the card's context *is* the work and a subagent would have to re-establish everything. That
was wrong in both halves.

The context a card needs is not the conversation — it's `add-card`, the SDK reference, and the card's
own Scryfall data, and a subagent re-establishes all three by loading a skill and making a fetch. It
pays a fixed setup cost once; the orchestrator that inlines the same work pays for the *output*
forever. And per-card research is the bulk of it: the Scryfall JSON, the printings list to settle
canonical placement, the greps through `mtg-sets` for a similar card, `examples.md`. None of that is
needed again once the file is written.

Delegating it also buys real parallelism, which the gate cannot. Card agents write disjoint files —
`{CardName}.kt` and `{CardName}ScenarioTest.kt` — so five run at once with nothing to contend over,
provided they stay out of the two shared resources: the git index and the backlog file. Hence the
division in the prompt: **card agents write files, the orchestrator commits, the gate agent does the
bookkeeping.** Explicit-path commits (never `git add -A`) keep one card's commit from sweeping up a
sibling's half-written file.

One rule gets simpler as a result. A card that ejects from the batch under `add-card` Step 4 used to
need its commit reset; now it was never committed, so the agent deletes its files and reports
`dropped`, and the orchestrator simply doesn't commit it.

What this costs is that no human-adjacent context ever sees the card code before the PR exists. The
review subagent is what makes that acceptable — it's mandatory, it's a fresh context that did not
write the code, and reading the diff without the author's assumptions is where "the code does what I
meant" bugs actually surface. That, the gate, and the `[agent-loop:]` title are the whole quality
story for an unattended run; none of the three is optional.

Subagents don't buy parallelism for the *gate*: `scripts/gradle-locked` caps the machine at two
concurrent Gradle runs, so a delegated gate blocks for as long as an inline one would. What changes
is only what comes back — a pass/fail line instead of the log.

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
not "Claude Code" — and not a subagent's own model, since the implement, gate and review subagents
may be running on something else entirely.

## Why batches

`CONTRIBUTING.md` sets the shape: cards built entirely from existing `Effects.*` / `Patterns.*` may share
a PR, while a card introducing new engine vocabulary gets one to itself with tests for the primitive. For
the loop that also means one gate slot covers five cards instead of one, which is most of the
wall-clock cost of a run — and with implementation fanned out, five cards are written in about the
time one takes.

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
`mtg-sets/src/test/resources/snapshots/cards/{SET}.json`. The `--jq` in the orientation step computes
exactly that test, so the orchestrator reads a boolean rather than a file list.

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
  worktree-rooted — including the ones you hand to subagents, which is why every dispatch in the
  prompt starts with the absolute worktree path. A compound command that `cd`s elsewhere resets the
  shell back afterwards, and a `just` gate run from the repo root does not fail with "no tests
  found" — it reports BUILD SUCCESSFUL for tests that never included your card. Check the Gradle
  problems-report path points into the worktree before believing a green.
- **`EnterWorktree` isolates the whole session.** From inside one, writes to a sibling worktree or to
  the main checkout are hard-blocked. That suits a loop owning one unit at a time; if the session is
  *already* isolated, branch inside it (`git checkout -b … main`) rather than adding a sibling you
  can create but never write to.
- **Worktrees don't buy unlimited parallel builds.** `scripts/gradle-locked` holds the machine to two
  concurrent Gradle runs and queues the rest for up to 30 minutes; three concurrent worktree builds
  have OOM'd the Kotlin daemon here. The worktree isolates *files*, not the build — so fan out card
  agents freely, and never fan out gates.

## Caveats

**The orchestrator never sees the code.** That's the point, and it's also the trade: the review
subagent and the gate are the only things standing between a wrong card and a merged PR. Read a
loop-produced PR accordingly — the label is there to tell you to.

**Compaction is still possible, just no longer fatal.** A long run can compact, and the summary will
lose detail the ledger keeps. Every iteration re-orients from open PRs, worktrees, and the ledger, so
a compacted session — or an entirely fresh one — resumes correctly. Nothing that matters lives only
in the conversation.

**Parallel card agents can converge on the same idea twice.** Five agents implementing five cards
don't see each other's work, so two cards in one batch may each invent a slightly different shape for
the same interaction. The review pass is where that gets caught; it's a reason to keep batches
thematic (shared colour, mechanic, or cycle) rather than arbitrary.

**Cards are not played.** No manual playthrough, no UX pass from both seats, no e2e. Each PR body
says so, and that pass is still the user's.

## See also

- [Codex `/goal` variant](set-implementation-goal.md) — same workflow, no subagents
</content>
