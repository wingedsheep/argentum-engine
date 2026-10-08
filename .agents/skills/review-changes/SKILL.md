---
name: review-changes
description: Review pending changes (a branch, PR, or working tree) for the Argentum Engine. A coordinator triages the diff by area and fans out parallel reviewer subagents — SDK (fit, reuse, elegance), Engine (correctness, performance, clean code), Server (client protocol, hidden information), Client (UX, consistency, dumb terminal), Cards & tests — then weighs their findings into one selective review ending in Approve or Request changes. Use when the user says "review this PR", "review this branch", "review my changes", or asks for a code review of pending work.
argument-hint: [<PR# | branch | path>]
---

# Review Changes

Primary lens: **SDK elegance**. The SDK must stay small and reusable so new cards compose
existing primitives instead of growing a card-specific type per Magic card.

You are the **coordinator**. You establish the diff, decide which area reviewers the change
needs, brief them, run the tests, and then weigh everything into one review. The reviewers
read and judge; you decide what the author actually sees.

The user's request or explicit skill arguments may contain a PR number/URL, a branch name,
or nothing (review the working tree's diff vs `main`).

## 1. Establish the diff (with `main` merged in)

The review must reflect post-merge reality, so `origin/main` is merged into the review
branch before diffing. Where the work happens depends on what's already checked out:

- **Already on the PR/branch in the current working tree** (`git rev-parse --abbrev-ref
  HEAD` matches the branch you were asked to review, working tree clean) → review in
  place. Skip the worktree step.
- **Anywhere else** (different branch checked out, dirty tree, PR head not fetched yet,
  user explicitly asks for a worktree) → use a dedicated worktree under
  `~/.claude/worktrees/argentum-engine/` (outside the repo, so an untracked worktree
  directory can't be staged into a commit by accident).

Steps:

1. **Resolve the PR / branch.** For a PR URL/number use
   `gh pr view <n> --json number,title,headRefName,baseRefName,headRepository,headRepositoryOwner,body`
   to learn the head repo + ref. For a bare branch name, skip to step 3.
2. **Fetch the head into a local review branch** (skip if already checked out and
   up-to-date). If the head is on a fork, fetch via HTTPS (this repo's `origin` is SSH
   and fetches against forks fail):
   `git fetch https://github.com/<owner>/<repo>.git <headRef>:pr-<n>-review`.
   Also refresh `origin/main`:
   `git fetch https://github.com/wingedsheep/argentum-engine.git main:refs/remotes/origin/main`.
3. **Pick the workspace.** Already on the branch with a clean tree → continue in place.
   Otherwise: `git worktree add ~/.claude/worktrees/argentum-engine/pr-<n>-review pr-<n>-review`
   (or `~/.claude/worktrees/argentum-engine/<branch>-review` for a branch). Create the
   parent dir first if needed (`mkdir -p ~/.claude/worktrees/argentum-engine`). Run
   subsequent commands in whichever workspace applies.
4. **Merge `origin/main` into the review branch** before reading the diff. This catches
   conflicts the author hasn't seen yet and ensures the review reflects post-merge
   reality: `git merge origin/main --no-edit`. If conflicts arise, resolve them (prefer
   main's structure for backlog/index files, then re-apply the PR's intent — e.g. bump
   the implemented-cards count, check the new card off the list) and commit. Flag the
   conflict resolution as a finding in the review so the author knows to either pull
   main themselves or accept the merge commit.
5. **Diff against `main`.** PR → `git diff origin/main...HEAD --stat` then full diff for
   source paths. Branch → `git diff main...<branch>` (three-dot). Empty → `git diff
   main...HEAD` plus `git status`.

Read the stat, the PR body, and enough of the diff to understand the change yourself — you
write the overview, and you can't triage what you haven't understood. The reviewers read
every file in their area in full.

**Worktree lifecycle (only when a worktree was created).** Leave it in place across
review rounds. Only remove it
(`git worktree remove ~/.claude/worktrees/argentum-engine/pr-<n>-review`) once the user
confirms the PR is merged or the review is abandoned. Mention the worktree
path in the final review output so the user can hand-off, re-enter, or push fixups to
it. When the review ran in place, the final output just notes that the branch already
has `origin/main` merged in (and any merge commit that produced).

## 2. Triage — which reviewers does this change need?

Map every changed path to an area:

| Area | Paths | Reviewer brief |
|------|-------|----------------|
| **SDK** | `mtg-sdk/`, `mtg-sdk-tooling/`, `docs/card-sdk-language-reference.md` | §3.1 |
| **Engine** | `rules-engine/` (except `engine/view/`), `ai/`, `gym*/` | §3.2 |
| **Server** | `game-server/`, `rules-engine/.../engine/view/` (`ClientStateTransformer`, `ClientEvent`) | §3.3 |
| **Client** | `web-client/` | §3.4 |
| **Cards & tests** | `mtg-sets/**/definitions/`, `mtg-sets/**/tests/`, `backlog/` | §3.5 |

Everything else (`oracle-assay/`, `mtgish-tooling/`, `justfile`, docs, skills) goes to
whichever reviewer it serves, or you review it yourself if it's small.

Then decide the fan-out:

- **One reviewer per touched area**, spawned together in a single message so they run in
  parallel (`Agent`, `subagent_type: general-purpose`). A new SDK primitive always pulls
  in the SDK reviewer even when only one line of `mtg-sdk/` changed — that line is the
  most consequential in the PR.
- **Split a large area into sub-areas** when one reviewer can't read it all in full —
  roughly more than ~1500 changed lines or ~25 files in one area. Split along seams that
  stand alone: engine → combat / layers & projection / triggers & continuations / costs &
  mana; client → per feature directory; cards → per set or per colour. Each sub-reviewer
  gets the same brief narrowed to its files, plus the list of the other sub-areas so it
  knows what's someone else's job.
- **Skip the fan-out for a trivial diff** — one area, a few dozen lines, no new SDK
  vocabulary. Review it yourself against the same brief; spawning costs more than it
  saves.
- **Cross-area wiring is yours.** A new `GameEvent` needs a `ClientEvent` branch and maybe a
  client animation; a new decision needs an engine executor, a server DTO and a client
  prompt. Reviewers see one side each — you check the chain is complete end to end.

### Shared prep before spawning

- **Rules text.** If the diff or PR body cites any CR number, download the Comprehensive
  Rules once (grab the `.txt` link from <https://magic.wizards.com/en/rules>, `curl -o`
  it into the scratchpad) and pass the local path to every reviewer so they can `grep`
  it instead of each fetching it.
- **Tests.** Start the test run yourself now, in the background, via the **`verify`**
  skill's `just` recipes — never raw `./gradlew`. Reviewers must **not** run builds or
  tests: parallel Gradle runs thrash the box. Run the broader module suite if a
  registry/executor/evaluator signature changed. Confirm green yourself; don't trust the
  PR description. The one exception: a caller that ran the `verify` gate itself on this
  exact head commit (the `set-loop` gate step) may tell you so — then don't re-run it;
  report that gate and its result instead.

### When you can't spawn the reviewers yourself

A subagent usually can't launch subagents of its own. If the `Agent` tool isn't available
to you, the review still happens per area:

- **Driven by an orchestrator (split mode).** The caller asks for one phase at a time and
  dispatches the area reviewers itself. Everything goes through a scratch directory the
  caller names (`<review-dir>`, gitignored — e.g. `<worktree>/.claude/loop-runs/review-pr-<N>/`):
  - **TRIAGE** — do §1 and §2 as written, but instead of spawning, write one brief per
    reviewer to `<review-dir>/<area>.brief.md` (areas and sub-areas named as in §2, e.g.
    `engine-combat`). Each brief is the full prompt from §3: workspace, base, owned files,
    PR summary, CR text path, the area section copied in, the output contract, and the
    instruction to write findings to `<review-dir>/<area>.findings.md`. Also write
    `<review-dir>/summary.md`: your "What the change does" paragraph, the cross-area wiring
    you need to check, and the test result. Return the list of area names.
  - **AREA** — a reviewer reads its brief file and follows it. It returns only its
    counts; the findings stay in the file.
  - **WEIGH** — read `summary.md` and every `*.findings.md`, do the cross-area wiring check,
    then §4 as written.
- **No orchestrator (sequential mode)** — e.g. a Codex session. Run §3 yourself one area
  at a time: finish an area's brief, write its findings down, then start the next, so each
  area gets a full read rather than a skim of everything at once. Then §4.

Either way, the "Reviewed by" line says how it ran (e.g. "SDK, Engine, Cards & tests —
split mode").

## 3. Reviewer briefs

Every reviewer prompt contains: the workspace path, the base (`origin/main`), the exact
file list it owns, a two-line summary of what the PR does (so it judges intent, not just
lines), the CR text path if any, the relevant brief below, and the **output contract**:

> Read every file you own in full, not just the hunks, and read the neighbouring code you
> compare against (the existing primitive, the sibling component). Do not edit files, run
> builds, or run tests. Return a list of findings, each with: severity (Blocking /
> Important / Minor), `file:line`, what is wrong, the concrete failure or cost it causes,
> the fix (for SDK shape issues, the rewritten card/DSL), and your confidence (confirmed
> by reading the code vs. plausible). Omit style nitpicks that don't need fixing. Also
> return one line on what in your area is genuinely good and worth keeping. If your area
> is clean, say so — an empty list is a valid answer.

### 3.1 SDK — does it fit, and is it the most elegant shape?

The bar is [`docs/sdk-design-principles.md`](../../../docs/sdk-design-principles.md) — the
same one `add-card` and `add-feature` write to. For every new SDK type the diff introduces
(`Effect`, `StaticAbility`, `Trigger`, `Condition`, `TargetRequirement`,
`EntityNumericProperty`, `DynamicAmount` variant, `Modification`, `ReplacementEffect`, …),
ask:

1. **Could the card compose existing primitives?** Tell-tale: the engine handler converts
   the new type 1:1 into an existing `Modification` / effect with a literal formula
   (`is NewThing -> ContinuousEffectData(Modification.X(literal), filter)`) where
   `Modification.X` already takes a `DynamicAmount`. Build the formula via
   `DynamicAmount.{Min,Max,Multiply,Add,EntityProperty,…}` and use the existing static
   ability (e.g. `GrantDynamicStatsEffect`).
2. **Is it genuinely novel?** Acceptable: reads state no primitive can read (new
   component, tracker, counter filter); player-interaction shape no executor produces;
   layer/timing semantics not expressible in the AST.
3. **Parameterized for the next card, not this one?** Constants baked in
   (`bonusPerType=1`, `maxBonus=10`, hardcoded subtype) → the next similar card forces
   another type. Prefer a small generic primitive + DSL recipe in a `*Patterns` object /
   `Conditions` / `Filters`.
4. **Atomic, modular, extendible?** Does the type do one thing (choosing split from
   acting), or bundle a pipeline that should be composed from atoms
   (`docs/architecture-principles.md` §1.5)? Would the *next* card of this family need a
   new field, or does it slot in?
5. **Could an existing type have taken the axis?** The order is compose → add an axis to
   the closest existing type → new type (`sdk-design-principles.md`, "Extend before you
   add"). Check the new type's `sdk-surface-baseline.txt` line: its two named neighbours
   and the reason are the claim to test. And if the change adds a general primitive, did
   it migrate and delete the narrow type it subsumes? A left-behind fossil is a finding.
6. **Name matches semantics?** `CreatureTypeCount` that counts all subtypes is a name
   lie — rename and document the gap. One spelling per concept: flag a second way to
   say something the SDK already says.
7. **Surface hygiene.** Cards reach it through facades (`Effects.*`, `Patterns.*`), not
   raw constructors (`FacadeBoundaryTest`); `docs/card-sdk-language-reference.md` is
   updated in the same change; the SDK holds data only — no execution logic.

When you find one, **show the rewrite**. A concrete card-using-existing-primitives is
more useful than abstract objection. References: `docs/architecture-principles.md` §1.5
(atomic pipelines), §1.2 (AST for dynamic values), §1.3 (composable filtering), §1.6 (DSL
as abstraction).

### 3.2 Engine — correct, clean, fast, and the home of all game logic

**Correctness — recurring bug classes in this engine:**

- **Projected vs base state** (`docs/architecture-principles.md` §2.3). Battlefield reads
  of type/subtype/color/keywords/P/T/controller MUST go through projection
  (`predicateEvaluator.matches(state, projected, …)`, `projected.getSubtypes`,
  `projected.isCreature`, `state.projectedState.getController`). Flag base
  `ControllerComponent` or `cardComponent.typeLine.isCreature` on battlefield permanents.
- **Layer 613.8.** New continuous-effect families: dependency-by-trial-application must
  hold; never `toMutableSet()` `ContinuousEffect` lists (dedupes equal lord effects).
- **Events, not silent mutations.** Every state change emits a `GameEvent`. Flag bypasses.
- **Trigger detection paths.** Battlefield → `detectTriggers`; phase/step →
  `detectPhaseStepTriggers` (called by the settle boundary, NOT `matchesTrigger`);
  leaves-the-battlefield → `detectLeavesBattlefieldTriggers`. Only `Settler` calls
  detection. Flag any handler, resumer or executor that detects or places triggers from
  its own events: it should emit the events and let the boundary queue them
  (`GameState.pendingTriggers`).
- **Last-known information.** Dies/leaves triggers must read `triggerLastKnownPower`,
  `lastKnownCardDefinitionId`, `lastKnownCounters` from `ZoneChangeEvent` (tokens
  disappear in the same SBA pass).
- **Continuations carry targets.** Frames wrapping `EffectTarget.ContextTarget(n)` must
  propagate `targets` / `namedTargets` / `outerTargets` into `EffectContext`.
- **Modal spells.** Check `modeTargetsOrdered` is built from flat `targets`, and
  no-target modes inherit outer targets.
- **Mana / costs.** New `ManaSource` shapes must reserve mana for self-activation costs.
- **Dispatch fallthrough.** A catch-all `else ->` in a `when` over a sealed type, or a fast
  path that skips a check the slow path makes, silently mishandles the next variant.
  Prefer exhaustive `when`s; flag fail-open defaults.
- **Immutability.** No in-place mutation of components; new state is returned.

**Game logic lives here, and only here.** No card-specific code in the engine (it
interprets SDK data); no rules decisions in the server or client. Flag logic that leaked
out of the engine, and card-name checks that leaked into it.

**Performance.** Flag work that scales badly on hot paths: recomputing projection inside a
loop over permanents, O(n²) scans of the battlefield per event, re-running trigger
detection or legality checks per candidate, allocating large collections per priority
pass. Legal-action enumeration and projection run constantly — cost there matters; cost in
a once-per-game setup path doesn't.

**Clean code.** Readable control flow, one responsibility per executor, reuse of the
existing services (`PredicateEvaluator`, cost payment, zone moves) rather than a parallel
reimplementation, names that say what the code does.

Consult the relevant doc when the change touches an area:
`continuous-effect-dependency-system.md`, `architecture-principles.md`, `player-input.md`.

### 3.3 Server — right protocol, nothing leaked

- **Anti-corruption layer.** A new `GameEvent` needs a branch in `ClientEvent.kt`'s
  exhaustive `when`; new client-visible state goes through `ClientStateTransformer`.
  DTO field names are stable JSON (pin them if a Kotlin rename would change the wire
  name); `docs/data-contracts.md` / `engine-server-interface.md` match.
- **Hidden information.** Nothing reaches a client that its player shouldn't see:
  opponents' hands, library order, face-down identities (morph, manifest, disguise),
  hidden choices (secret creature-type / name choices), cards looked at privately, another
  player's pending decision contents. Check the masking path for *every* player seat,
  spectators and replays included. Visibility rules are an engine concern too — if the
  engine emits the secret in an event payload, masking downstream is fragile; say which
  layer should own it.
- **Server is authoritative.** Legal actions come from the server; the server validates
  every client-supplied `GameAction` rather than trusting it.
- **Orchestration.** Session / lobby / tournament code: reconnect paths, concurrency on
  shared game state, silent `return false` failures, errors surfaced to the client
  instead of swallowed.

### 3.4 Client — good UX, consistent, dumb

- **UX.** Is the flow intuitive for a player who doesn't know the implementation? Is the
  prompt clear about what's being chosen and why? Does it prefer selecting on the
  battlefield over a modal list where that's natural? Are battlefield positions stable
  (nothing jumps around)? Does it work in multiplayer / 2HG layouts and at narrow widths?
- **Consistent with the rest of the frontend.** Compare with sibling components under
  `web-client/src/components/` (decisions, targeting, game, shared): same patterns,
  styling approach, store usage, naming. Flag a new component that duplicates one that
  already exists (a second card preview, a second choice modal) — reuse is the bar.
- **Dumb terminal.** No game logic in `web-client`: no computing legal actions, targets,
  costs or rules outcomes; the server sends them. Display-only derivation is fine.
- **Clean code.** Easy to follow components, state in the right place (Zustand store vs
  local), no dead props, types matching the server DTOs. See
  `docs/web-client-architecture.md`.

### 3.5 Cards & tests

**Printing placement.** For every card whose `CardDefinition` or `Printing(...)` row is
added or moved, the coordinator runs `just check-card-printing "<Card Name>"` (list the
cards for them; it's a script, not a Gradle build). It exits non-zero unless the canonical
`card("Name") { ... }` lives in the card's **earliest real-expansion printing** (per
Scryfall, skipping `promo` / `token` / `art_series`) and every other scaffolded printing
has a `Printing(...)` row in its set's `cards/` package. If the earliest real set isn't
scaffolded under `mtg-sets/.../definitions/<setcode>/`, the expectation is to scaffold it
(a minimal `MtgSet` object under `definitions/` — `MtgSetCatalog` discovers it on the
classpath, there is no registration list) and host the canonical there. **Blocking** if
the diff put the canonical in a later set without scaffolding the original, unless the PR
body documents why that's out of scope.

**Oracle fidelity.** Cross-check each card against Scryfall Oracle
(`https://api.scryfall.com/cards/named?exact=<card>` — fetch with Python `urllib` if
`WebFetch` is blocked); the PR may implement pre-errata wording. Spell out the rules path
for corner cases (Changeling, copyable values, layer interactions, "as ~ enters",
protection / hexproof / ward).

**Tests — every cited rule must be exercised.**

- **Coverage of cited rules.** Every rule the implementation references in code or
  comments must have a test that exercises it. If the change cites "trample through dead
  blockers", there must be a test where the attacker has trample and a blocker dies;
  otherwise the citation is decorative.
- **Interesting axes.** Typical case + the rule-corner that drove the change (Changeling
  for type-counting; regeneration for destroy-vs-exile; last-known-info for dies
  triggers; first-strike + trample interaction; etc.). A test that would pass without the
  change doesn't test it.
- **One card, one test file.** `<CardName>ScenarioTest.kt` per card; flag batched test
  files (mechanic-level engine tests are the exception).
- **Module placement.** Card and mechanic behavior → scenario tests (the engine is the
  source of truth). SDK round-trips → `mtg-sdk`. A `game-server` test is correct only for a
  genuine game-server concern — state masking, DTO transformation, session/tournament
  orchestration. **Flag any `game-server` scenario test written to prove engine
  behavior.** JSON round-trip fixtures are NOT required per card.
- **`ScenarioTestBase` set scope.** Only registered sets are loaded; cards from other sets
  must be defined inline via `CardDefinition.creature(...)` and registered via
  `cardRegistry.register(card)` in `init { }`.

### Every reviewer, whatever the area

- **MTG rule numbers.** Verify every CR number cited in code, comments, commit messages or
  the PR body against the downloaded rules text. Numbers are easy to swap (613.7 vs 613.8,
  704.5 vs 704.6, 608.2b vs 608.2c); a mismatch is an Important finding with the correct
  number. If it can't be verified, recommend describing the rule by name.
- **Style & scope.** Comments only when *why* is non-obvious — flag restated-code comments
  and "added for X" notes. No backwards-compat hacks (unused fields, `// removed`
  markers). Code reads like its surroundings.

## 4. Weigh and report

When the reviewers and the test run are back:

1. **Verify before you forward.** Re-read the cited code for every Blocking and Important
   finding. Drop what doesn't hold up; downgrade what's real but overstated. A reviewer's
   "plausible" becomes a finding only once you've confirmed it.
2. **Merge and dedupe.** The same root cause seen from two areas (engine leaks a secret in
   an event; server doesn't mask it) is **one** finding, placed at the layer that should
   fix it. Add your own cross-area wiring findings (§2).
3. **Rank and cut.** Order by what matters most for merging: wrong behaviour and broken
   rules, then hidden-information leaks, then SDK shape that will cost every future card,
   then missing tests, then performance, then clarity. **Be selective** — drop anything so
   minor it doesn't really need fixing. Five findings that matter beat fifteen that dilute
   them. Failing tests are always a finding, with the failure output.

### Output

Lead with the overview. The author needs to see that you understood the change before
they'll trust a single finding, and a reviewer who can't summarize the diff hasn't read
it.

**1. What the change does** — the *idea* of the PR, written from the diff itself, before
any judgement. Two to five sentences of prose: what problem it solves or which cards /
mechanic it enables, the one-line shape of the approach, and what the tests establish.
Pitch it at "what would you tell a colleague who asked what this PR is about".

This is a summary, **not** a file-by-file walkthrough. Do not list changed files, line
counts, class or method names, or per-file summaries. Name a module or a type only when the
idea is unintelligible without it, at most once or twice. Describe, don't evaluate — no
praise, no findings, no "but". If the PR body claims something the diff doesn't do, note
the discrepancy here as a plain fact. Add one line naming which reviewers ran (e.g. "Reviewed
by: SDK, Engine (combat, triggers), Cards & tests") and the test result.

**2. Findings, most important first** — one numbered list, not grouped by area. Each
finding is tagged with its severity and area, e.g. **1. [Blocking · Engine]**, then one
short paragraph with `file:line`, what goes wrong (the concrete scenario), and the concrete
fix — for SDK shape issues, the rewritten card or DSL.

- **Blocking** — wrong behavior, broken rules, hidden information exposed, missing wiring
  (new event without `ClientEvent.kt` branch), tests that don't exercise the change,
  base-vs-projected state bugs, failing tests.
- **Important** — over-specialized SDK types, CR-number mismatches, missing rule-corner
  test, game logic in the wrong layer, UX that will confuse players, a real performance
  cost on a hot path, naming that lies about semantics.
- **Minor** — only if it's worth the author's time to fix: a misleading comment, a doc out
  of sync, dead code. Real nitpicks are left out entirely.

If there are no findings, say so in one line.

**3. Verdict** — **Approve** or **Request changes**, with one or two sentences: the
concrete next action ("drop type X, define the card via Y, add a test for Z"), and — when
requesting changes — what's worth keeping through the rewrite (clean tests, right
plumbing, well-chosen primitives) so the author doesn't throw it away. Any Blocking or
Important finding means Request changes; Minor-only means Approve.

Close with the workspace note from §1 (worktree path, or "reviewed in place with
`origin/main` merged in").

This skill writes a review into the conversation. It does not push, run `/ultrareview`,
auto-fix, or touch other agents' work. It posts via `gh` only when the caller asks for it
(the `set-loop` review step does): then the whole review — overview, findings, verdict — goes
up as one PR comment with `gh pr comment <N> --body-file <file>`, headed with the verdict so
a later fixer can find it. The worktree from step 1 stays in
place after the review so the author (or a follow-up session) can iterate on it.
