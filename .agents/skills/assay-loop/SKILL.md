---
name: assay-loop
description: Launch a self-continuing agent loop that widens Argentum Assay's grammar one band per reviewed PR, teaching it to read Oracle text the engine/SDK already expresses — declines on cards we have already hand-written, never new SDK vocabulary. Composes the loop prompt with the driving model id and the PR labelling rule, like `set-loop`. Use when asked to "make Assay more complete", "keep adding bands to Assay", "close the Assay ⇄ engine gap", or "loop over Assay's decline backlog".
argument-hint: "[optional focus, e.g. a gap-analysis theme or SDK family]"
---

# Assay loop

Closes the gap between what the engine can do and what Assay can read, one **band** per PR — a band is
one decline family taught to the grammar (`oracle-assay/.../grammar/`), in both directions, landing on SDK
values hand-written cards already use. You **compose and launch** the loop prompt; you don't write grammar
in this turn.

**Prefer `just assay-loop [model] [focus]`** when the user can run it in a terminal: it is the same loop
driven by `scripts/assay-loop`, one fresh headless session per step (no context growth), waits out CI and
usage limits for free, and stops on `touch .claude/loop-runs/assay.stop`. This skill is the in-session
`/loop` variant.

It is `set-loop`'s shape with a different unit. Read [`set-loop`](../set-loop/SKILL.md) for the harness
table, the `<MODEL_ID>` resolution rule and why the PR label is mandatory — all three apply unchanged.
The flat-orchestrator reasoning is in
[`docs/agent-loops/set-implementation-loop.md`](../../../docs/agent-loops/set-implementation-loop.md#how-the-driving-session-stays-flat).

## What makes a unit eligible

The loop's scope is **"already implemented"**, and it is enforced by *where the work list comes from*:

- **The backlog is `just assay-report --implemented --rank tail`** — declines on cards that already have a
  golden. Every such line has a hand-written reading, so the SDK provably expresses it and the
  differential has an answer key for the band.
- **No `mtg-sdk`, `rules-engine` or `mtg-sets` *vocabulary* change.** If a family can only be read by
  adding an SDK type, field or facade, it is `add-feature` work: the loop records it `needs-sdk` and moves
  on. Card *fixes* the differential proves wrong are the exception — they ship in the band's PR, as every
  previous band's did.
- **Out of scope by design** (record `[-]`, never pick): multi-face layouts, ability-word markers,
  presentation fields, and the "two SDK spellings, one canonical" minority — the list is in
  `backlog/assay-engine-gap-analysis.md` § *Not gaps*, when that file exists.
- **Probe before picking.** A tail rank names lines a family *touches*, not cards it *finishes*
  (`oracle-assay/README.md`, and the four overstatements behind it). The pick is by the explorer's
  prefix probe — whole cards finished on the live grammar — not by the rank.

## Step 1 — resolve the substitutions

**`<MODEL_ID>`** — exactly as `set-loop` Step 1: the literal id of the model driving the loop, context
suffix dropped. Ask once if you can't determine it; never guess.

**`<FOCUS>`** — optional. A theme from the gap analysis ("kicker / cast-history conditions"), an SDK
family (`CardPredicate`), or `-` for "whatever the probe ranks highest". A focus narrows the PICK stage;
it never widens the scope rules above.

## Step 2 — launch

Substitute `<MODEL_ID>` and `<FOCUS>` throughout, then send the wrapper for the current harness.
Claude Code:

```
/loop Widen Argentum Assay's grammar one band per PR, over Oracle text the engine already expresses, and label every PR as loop-produced. Focus: <FOCUS>.

You are the ORCHESTRATOR. You dispatch, you decide, you record. You do NOT read grammar files, diffs, gate output, rankings, or review bodies — a subagent reads those and hands you back a short verdict block. Twenty bands must cost your context roughly what two cost.

ORIENT — cheaply, every iteration. The ledger is your memory between turns; this conversation is not.
- Read `.claude/loop-runs/assay-grammar.md`. If it doesn't exist, create it with a `# Loop run: assay-grammar` header, the line `legend: [ ] pending · [~] implementing · [r] in review · [c] correcting · [x] done · [!] needs human · [s] needs-sdk · [-] out of scope`, and a `## Bands` section. It is gitignored — never commit it.
- `gh pr list --author @me --state open --json number,title,headRefName,files,reviewDecision,statusCheckRollup --jq '.[] | "#\(.number) \(.headRefName) review=\(.reviewDecision // "none") checks=\([.statusCheckRollup[]?|.conclusion // .state]|unique|join(",")) mine=\([.files[].path]|any(test("^oracle-assay/src/main/kotlin/com/wingedsheep/assay/(grammar|normalize|syntax)/")))"'` — one line per PR, never the raw JSON. A PR is yours only when `mine=true`. Other people's PRs are NOT yours to advance.
- `git worktree list`, and the current branch.

WORKTREE — never work in the shared main checkout. If one already holds the in-flight band branch, use it; otherwise `git worktree add .claude/worktrees/assay-grammar -b worktree-assay-grammar main`. Pass that absolute path to every subagent and run every `just` gate, commit and `gh` call from it. Every `oracle-assay` binary a subagent runs must be built from that worktree (`just assay …` rebuilds it); a stale binary from another checkout makes every before/after number meaningless.

Then advance exactly ONE step:

1. No Assay band PR of ours open → build the next band.
   a. PICK — one subagent: "In <worktree>, run `just assay-report --implemented --rank tail --top 40`. Read `.claude/loop-runs/assay-grammar.md` and skip every family already marked there (any status). If `backlog/assay-engine-gap-analysis.md` exists, treat its 'Not gaps' section as out of scope, and use its themes to bias toward <FOCUS> when that is not '-'. For the top ~8 remaining families, measure what each would FINISH: start `just assay-explore --no-open --port 7399` in the background, and POST `{"by":"tail","key":"<family key>","find":"<span>","replace":"<a replacement the grammar already reads>","regex":"false"}` to `http://127.0.0.1:7399/api/probe`; stop the server when done. For each candidate, check the SDK values its lines need already appear in the hand-written goldens (`mtg-sets/src/test/resources/snapshots/cards/`) — if reading it needs a NEW SDK type/field/facade, it is needs-sdk, not a candidate. Choose the family with the most whole cards finished (ties: fewer grammar files touched). Append the band line to the ledger, plus any needs-sdk or out-of-scope family you found, each with a one-line reason. Return only: BAND (short name) / KIND (band|none-left) / KEY (tail key) / PROBE (N lines, N whole cards) / SDK (the model values it lands on) / REASON (one line)."
   b. IMPLEMENT — ONE subagent (grammar files are shared; never fan out): "In <worktree>, add the '<BAND>' band to Argentum Assay for tail family <KEY>. First read `oracle-assay/README.md` ('The three things that make this different', 'Reading the verdicts', and the two most recent band sections) and `docs/sdk-design-principles.md`. BASELINE before any edit, with the worktree's own binary: `just assay-report --implemented | tail -25` (whole cards) and `just assay-differential | tail -8` (compared / confirmed / divergent). Then write the rules under `oracle-assay/src/main/kotlin/com/wingedsheep/assay/grammar/`: every rule both builds and matches, parses straight into existing `mtg-sdk` types through the same companion factories / facades the hand-written cards use, and reuses existing slots before adding one. Do NOT change `mtg-sdk`, `rules-engine`, or add SDK vocabulary — if the band cannot be read without it, revert your edits and report needs-sdk. Add grammar tests beside the existing ones in `oracle-assay/src/test/`. Then: `just assay-gate` must report 0 mismatch, 0 ambiguous, 0 non-invertible. `just assay-differential`: classify ONLY the new divergences against your baseline — parser bug (fix the grammar), card bug (fix the card to the reading, CR-checked, then `just rebless-cards` and confirm only those cards moved), or standing SDK finding (leave it, note it). A divergence you cannot classify is a stop. Add a short band section to `oracle-assay/README.md` in the house style (what the family was, the probe vs delivered, what it found). Commit in this order, by explicit path, never `git add -A`: grammar + tests + README; card fixes + goldens (if any); then `just assay-bake` and the ledger JSON on its own. Return only: BAND / STATUS (done|needs-sdk|failed) / WHOLE (before -> after) / PROBE-VS-DELIVERED / DIFFERENTIAL (before -> after, N parser-fixed, N card bugs, N SDK findings) / COMMITS (N) / NOTE (one line)."
   c. GATE — one subagent: "In <worktree>, run `just build` (it covers `:oracle-assay` tests and the card snapshot test) and `just assay-gate`. Tee both to <worktree>/gate.log. Return only: GATE / STATUS (passed|failed) / FAILING (test names, or -)."
   d. Green → push and open the PR with `gh pr create --title "[agent-loop: <MODEL_ID>] Assay: <BAND> band"`. That prefix is mandatory on every PR this loop opens, and <MODEL_ID> is the model driving this loop, not a subagent's own model. Write the body from the verdict blocks you hold: the family and its tail key, probe vs delivered, whole cards before -> after, differential before -> after with the classification counts, any card bugs fixed by name, and what was NOT checked (no scenario playthrough of fixed cards beyond their existing tests). Say it came from an agentic loop driven by <MODEL_ID>. Red → dispatch ONE fix subagent with the FAILING names and the log path; still red → mark the band `[!]` and stop for a human.
   needs-sdk → the implement subagent has already reverted its edits; mark the band `[s]` with the reason and PICK again next iteration.
2. Our band PR is open and unreviewed → REVIEW, following /review-changes in split mode exactly as /set-loop step 2 does (TRIAGE, one subagent per AREA, WEIGH, one comment on the PR). Record the verdict and counts in the ledger; do not read the briefs, findings, or comment.
3. Our band PR's review requested changes → FIX — one subagent: "In <worktree>, read the review comment on PR #<N> via `gh pr view <N> --comments`, fix what holds up, decline what doesn't and reply on the PR saying why, re-run `just assay-gate` and `just build`, re-bake with `just assay-bake` if the grammar changed, and push to the same branch. Return only: PR / FIXED (N of N, N declined) / GATE (passed|failed) / STATUS (pushed|needs-human)."
4. Our band PR is approved, or its findings are fixed or declined, and its checks are green → `gh pr merge --squash --delete-branch` from the main checkout, `git worktree remove` the worktree, switch to main and pull. Mark the band `[x]` with the PR number and the delivered whole-card delta.

VERDICT DISCIPLINE — every dispatch names exactly what comes back, and that is all you keep. If a subagent returns prose, take the first status word and move on; never read its transcript.

RULES: one band per PR, never two band PRs in flight. Grammar work is never parallel. Gates run through `just`, never raw ./gradlew. No SDK/engine vocabulary changes, ever — that is /add-feature, and a needs-sdk family is a finding for the ledger, not a reason to widen the loop. mtgish is irrelevant here and its failures never block. Never revert or stash changes you didn't make; if someone else's work breaks the build, report it and stop. Don't retry a failed subagent more than once — mark `[!]` and move on; three consecutive failures or three consecutive needs-sdk picks mean the ranking is exhausted for this scope, so stop and report. When PICK returns none-left, report the ledger's totals and stop the loop.
```

**Codex** takes the same body under `/goal` with the three changes `set-loop` lists: prepend the skill file
paths, collapse the dispatch list into inline steps, and end with a completion criterion ("PICK finds no
in-scope family that finishes a whole card").

Then tell the user the loop is running, its focus, and the label its PRs will carry.

## Why it's shaped this way

- **`--implemented` is the whole scope rule.** A decline on a card we already wrote is, by construction,
  text the engine expresses — and the only kind the differential can check. A decline on an unwritten card
  may need vocabulary nobody has built; that is `add-feature`, then `add-card`, then this loop.
- **The probe picks, not the rank.** Every band that trusted a rank delivered a fraction of it; the probe
  has been within ~1.3× when the family owns the line. PICK measures ~8 candidates so one bad estimate
  doesn't steer a whole PR.
- **Baseline inside the worktree, before the edit.** The differential has never been a fixed zero — it
  rises when the grammar reaches a new card class, and every such rise so far has been a real card bug.
  A band is judged on the *delta* it classifies, which needs a baseline from the same binary.
- **Two re-blesses, separate commits.** `just rebless-cards` (goldens, only for card fixes) and
  `just assay-bake` (the verdict ledger behind the Set Completion view's ⚡ badge) move for different
  reasons; keeping them apart keeps the review readable.
- **One implementer, never a fan-out.** Bands collide in `Steps`, `Filters` and `Primitives`; two agents in
  the grammar at once is an ambiguity waiting for the gate.

## Caveats to pass on

- **Progress lives in `.claude/loop-runs/assay-grammar.md`** (gitignored), including every `needs-sdk`
  family — which is, read the other way, a ranked list of `add-feature` candidates.
- **The orchestrator never reads the grammar.** Review plus the three gates (touchstone, differential,
  build) are the quality story; the `[agent-loop:]` title tells a human to read accordingly.
- **Card bugs are real behaviour changes.** A band that fixes a card changes how it plays; those are
  named in the PR body so they get read.
