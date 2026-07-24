---
name: review-changes
description: Review pending changes (a branch, PR, or working tree) for the Argentum Engine. Optimizes for an elegant, reusable SDK — flags one-off effects/abilities that should compose existing primitives — and checks correctness, projection use, tests, and architectural fit. Use when the user says "review this PR", "review this branch", "review my changes", or asks for a code review of pending work.
argument-hint: [<PR# | branch | path>]
---

# Review Changes

Primary lens: **SDK elegance**. The SDK must stay small and reusable so new cards compose
existing primitives instead of growing a card-specific type per Magic card.

The user's request or explicit skill arguments may contain a PR number/URL, a branch name, or nothing (review the working tree's
diff vs `main`).

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

Read every changed file in full, not just the hunk. For large diffs, spawn `Explore` for
unfamiliar areas.

**Worktree lifecycle (only when a worktree was created).** Leave it in place across
review rounds. Only remove it
(`git worktree remove ~/.claude/worktrees/argentum-engine/pr-<n>-review`) once the user
confirms the PR is merged or the review is abandoned. Mention the worktree
path in the final review output so the user can hand-off, re-enter, or push fixups to
it. When the review ran in place, the final output just notes that the branch already
has `origin/main` merged in (and any merge commit that produced).

## 2. SDK elegance — the central question

For every new SDK type the diff introduces (`Effect`, `StaticAbility`, `Trigger`,
`Condition`, `TargetRequirement`, `EntityNumericProperty`, `DynamicAmount` variant,
`Modification`, `ReplacementEffect`, …), ask:

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
4. **Name matches semantics?** `CreatureTypeCount` that counts all subtypes is a name
   lie — rename and document the gap.

When you find one, **show the rewrite**. A concrete card-using-existing-primitives is
more useful than abstract objection.

Reference: `docs/architecture-principles.md` §1.5 (atomic pipelines), §1.2 (AST for
dynamic values), §1.3 (composable filtering), §1.6 (DSL as abstraction).

## 2b. Printing placement for new / reprinted cards

For every card whose `CardDefinition` or `Printing(...)` row is added or moved in the
diff, run `just check-card-printing "<Card Name>"`. The script lists all Scryfall
printings and exits non-zero unless:

- the canonical `card("Name") { ... }` lives in the card's **earliest real-expansion
  printing** (per Scryfall, skipping `promo` / `token` / `art_series`), and
- every other scaffolded printing has a `Printing(...)` row in its set's `cards/` package.

If the earliest real set isn't scaffolded under `mtg-sets/.../definitions/<setcode>/`,
the script reports it as drift. The expectation in that case is to scaffold the earliest
set (minimal `MtgSet` object + `META-INF/services` entry) and host the canonical there.
Flag it as **Blocking** if the diff put the canonical in a later set without scaffolding
the original; the only acceptable miss is when the author documents in the PR body why
scaffolding the earlier set is out of scope.

## 3. Correctness — recurring bug classes in this engine

- **Projected vs base state** (`docs/architecture-principles.md` §2.3). Battlefield reads
  of type/subtype/color/keywords/P/T/controller MUST go through projection
  (`predicateEvaluator.matchesWithProjection`, `projected.getSubtypes`,
  `projected.isCreature`, `state.projectedState.getController`). Flag base
  `ControllerComponent` or `cardComponent.typeLine.isCreature` on battlefield permanents.
- **Layer 613.8.** New continuous-effect families: dependency-by-trial-application must
  hold; never `toMutableSet()` `ContinuousEffect` lists (dedupes equal lord effects).
- **Events, not silent mutations.** Every state change emits a `GameEvent`. Flag bypasses.
- **Trigger detection paths.** Battlefield → `detectTriggers`; phase/step →
  `detectPhaseStepTriggers` (called by `PassPriorityHandler`, NOT `matchesTrigger`);
  leaves-the-battlefield → `detectLeavesBattlefieldTriggers`.
- **Last-known information.** Dies/leaves triggers must read `triggerLastKnownPower`,
  `lastKnownCardDefinitionId`, `lastKnownCounters` from `ZoneChangeEvent` (tokens
  disappear in the same SBA pass).
- **Continuations carry targets.** Frames wrapping `EffectTarget.ContextTarget(n)` must
  propagate `targets` / `namedTargets` / `outerTargets` into `EffectContext`.
- **Modal spells.** Check `modeTargetsOrdered` is built from flat `targets`, and
  no-target modes inherit outer targets.
- **Mana / costs.** New `ManaSource` shapes must reserve mana for self-activation costs.
- **Anti-corruption layer.** New `GameEvent` → branch in `ClientEvent.kt` exhaustive
  `when`. New client-visible state → `ClientStateTransformer`.
- **Dumb-terminal client.** No game logic in `web-client`; server sends legal actions.

Consult the relevant doc when the change touches an area:
`continuous-effect-dependency-system.md`, `engine-server-interface.md`,
`data-contracts.md`, `card-sdk-language-reference.md`, `api-guide.md`.

## 4. MTG rules accuracy — verify every cited rule

Whenever the diff (code, comments, commit message, or PR body) cites a CR rule number,
**verify it online** before accepting it:

```
WebFetch https://magic.wizards.com/en/rules   # official WotC rules page; grab the .txt link, then `curl` it down and grep locally (too large to fetch into context)
WebFetch https://api.scryfall.com/cards/named?exact=<card>&set=<code>   # Oracle text
```

CR numbers drift between editions and are easy to misremember (613.7 vs 613.8, 704.5 vs
704.6, 608.2b vs 608.2c). If the cited number doesn't match the rule's text, fix it in
the review (and request the author fix it in code/commit). When in doubt, describe the
rule by name rather than guessing a number.

For card text, cross-check Scryfall Oracle — the PR may be implementing pre-errata
wording. Spell out the rules path you expect for corner cases (Changeling, copyable
values, layer interactions, "as ~ enters", protection / hexproof / ward).

## 5. Tests — every cited rule must be exercised

- **Coverage of cited rules.** Every rule the implementation references in code or
  comments must have a test that exercises it. If the change cites "CR 702.19c trample
  through dead blockers", there must be a test where the attacker has trample and a
  blocker dies; otherwise the citation is decorative. Flag rule-citations without a
  paired test case.
- **Interesting axes.** Typical case + the rule-corner that drove the change (Changeling
  for type-counting; regeneration for destroy-vs-exile; last-known-info for dies
  triggers; first-strike + trample interaction; etc.).
- **Module placement.** Card-interaction scenarios → `game-server` `scenarios/` (full
  stack). Pure rules → `rules-engine` `scenarios/` with `ScenarioTestBase`. SDK round-
  trips → `mtg-sdk`. JSON round-trip fixtures are NOT required per card.
- **`ScenarioTestBase` set scope.** Only registered sets are loaded; cards from other
  sets must be defined inline via `CardDefinition.creature(...)` and registered via
  `cardRegistry.register(card)` in `init { }`.
- **Run the tests yourself.** `./gradlew :game-server:test --tests "<Name>"`,
  `:rules-engine:test`, `:mtg-sdk:test` after engine/SDK changes. Confirm green; don't
  trust the PR description. Run the broader module suite if a registry/executor/evaluator
  signature changed.

## 6. Style & scope

- Comments only when *why* is non-obvious (project AGENTS.md). Flag restated-code
  comments and "added for X" notes.
- No backwards-compat hacks (unused fields, `// removed` markers).

## 7. Output

1. **Verdict** (1–2 sentences) — is the behavior right? Is the SDK shape right?
2. **What's good** — genuine positives worth keeping if the author rewrites: clean tests,
   right plumbing, good naming, well-chosen primitives. Skip filler; if there's nothing
   real to praise, say so briefly. Do this *before* the issues so the author knows what
   not to throw away.
3. **Issues, by severity:**
   - **Blocking** — wrong behavior, broken rules, missing wiring (new event without
     `ClientEvent.kt` branch), tests that don't exercise the change, base-vs-projected
     state bugs. Must be fixed before merge.
   - **Important** — over-specialized SDK types (show the rewrite), CR-number mismatches,
     missing rule-corner test, projection fallback gaps, naming that lies about
     semantics. Should be fixed before merge.
   - **Minor** — comment hygiene, descriptions, drive-by formatting, dead code, doc
     inconsistencies. Author's discretion.

   Each issue: one short paragraph with `file:line` and the concrete fix.
4. **Recommendation** — concrete next action ("drop type X, define card via Y, add a
   test for Z"). If the diff is fine as-is, say so.

This skill writes a review into the conversation. It does not push, post via `gh`, run
`/ultrareview`, auto-fix, or touch other agents' work. The worktree from step 1 stays
in place after the review so the author (or a follow-up session) can iterate on it.
