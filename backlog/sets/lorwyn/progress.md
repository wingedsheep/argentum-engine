# Lorwyn completion work

This work tracks completion of Lorwyn and the engine capabilities its cards require. The initial completion PR #2235 was merged at 251/286 cards. Further work continues in draft PRs; the set is not yet complete.

## Starting point

- Base: `4f09fec7e2` (merged clash support and five Lorwyn cards).
- `scripts/card-status --set LRW`: 240 / 286 unique draft cards, 46 missing.
- Twenty basic-land printings already exist; the five names count toward the 286-card checklist.
- Isolated worktree: `.claude/worktrees/lrw-completion`; branch: `codex/lrw-completion`.

## Completion requirements

- [ ] Implement every remaining card in `cards.md`, including required engine, server, and client behavior.
- [ ] Audit previously implemented cards, including the hideaway resolution and timing concerns in `mechanics.md`.
- [ ] Verify canonical printings, complete Lorwyn reprint coverage, and all basic-land arts.
- [ ] Run appropriate build, engine, scenario, serialization, and UI gates for each change.
- [ ] Review all snapshot changes and classify Assay differential disagreements.
- [ ] Run the complete `verify-set` workflow: field verification, script and token review, behavioral/self-play checks, and completion report.
- [ ] Remove the set's incomplete flag only after the completion evidence supports it; archive the backlog after verification.

## Work in progress

Eleven new cards are implemented: Bog Hoodlums, Nath's Elite, Fistful of Force, Spring Cleaning, Woodland Guidance, Sentry Oak, Springjack Knight, Whirlpool Whelm, Hoarder’s Greed, Gilt-Leaf Ambush, and Hunt Down. All compose existing primitives. Canonical-printing checks, fresh Scryfall fields, image HTTP 200 checks, and snapshot validation passed. Source completeness is now 251 / 286 (35 missing). Seven focused scenario tests for the three spells passed, covering clash win/loss, target preservation, temporary bonuses, destruction scope, graveyard targeting, untapping, and self-exile.

The differential audit also found and fixed seven existing Harbinger bugs: declining the optional search incorrectly forced a search and shuffle. All seven now gate the entire search. Their 21 per-card regression tests passed. At that stage, snapshots were compared field by field: only the five new cards and seven optional gates changed from the starting point.

The earlier five-card batch's Assay audit agrees for 108 / 111 compared canonical cards; three equivalent static-ability folds remain (see `assay-review.md`). It declines 126 cards, including all five new clash cards, and two more cards fail to fold. Those results do not verify behavior for the uncovered cards.

The earlier full builds failed only on expected new-card snapshot additions. Regeneration passed and the final `just build` for this batch passed (3m 27s, 128 tasks). No manual playthrough or end-to-end UI test has run. Champion research and required edge cases are recorded in `champion.md`; the mechanic is not implemented yet.

The linked-exile source-visit prerequisite is now implemented. An old leaves trigger reads its
original source visit's pile after a blink or token cleanup; leaving exile invalidates previous
links. Seven engine scenarios passed, including a full game-state serialization round trip.
`just test-rules` passed (2m 19s, 61 tasks). This internal state change reuses existing events and
selection UI; no client interaction was added. Champion itself remains unimplemented: distinct
linked ability pairs, champion events, and the remaining behavioral matrix are tracked
in `champion.md`.

The next source-reference prerequisite is verified: old abilities do not act on a source that has
already returned, while effects can still return and then modify their own source. Source-relative
filters distinguish battlefield visits too. Sentry Oak and Springjack Knight have eight individual
card scenarios covering clash results, timing, decline, target loss, duration, and absent/returned
sources. Their printing checks and fresh Scryfall/image rechecks passed. Snapshot comparison found
only these two additions and no changes to existing entries. `just test` passed for the combined
batch (4m 27s, 106 tasks), including engine, card, SDK, AI, gym and server suites. Both new cards'
clash lines are declined by Assay; the generator supplies scaffolds rather than complete scripts.

## Whirlpool Whelm and Hoarder's Greed

Both cards now have four passing scenarios. Whelm tests target selection before the clash,
its win-only destination choice after both library decisions, the opponent's library ordering,
and a fully illegal target preventing the clash. Greed tests automatic repetition on a win and
termination on a loss or tie, with the life loss and draws preceding each clash. The standalone
clash helper composes existing effects; every pre-existing snapshot entry remains unchanged.
Whirlpool Whelm also has its required Commander reprint row. Fresh Scryfall fields, image
HTTP 200 checks, and canonical-printing checks passed. Source completeness is 249 / 286.

The full gate completed with one unrelated 120-second timeout in AbattoirGhoulScenarioTest;
all eight new scenarios passed. After user authorization to continue, that test passed in
isolation without code changes (1m 16s, 43 tasks). The full `just test` retry passed (1m 21s, 106 tasks). The fresh Assay differential covers 243 canonicals: 111 compared,
108 agree, the same three previously verified equivalent static folds, 130 declined, and two
failed to fold. Neither new card is covered by the grammar.

## Gilt-Leaf Ambush

The card creates its tokens before the clash and grants temporary deathtouch only to the
created-token collection. All three scenarios pass: win, loss, and a win with Doubling Season
creating four tokens. They also prove another Elf is unaffected and the keyword expires.
The card uses existing token, clash, collection-iteration and keyword primitives. Fresh card
fields, canonical placement, card art and Lorwyn Elf Warrior token art are verified. Snapshot
regeneration passed with only this card added and no existing entry changed. The required
`just build` gate passed (4m 27s, 128 tasks). Source completeness is 250 / 286, with 36 remaining.

Fresh Assay differential: 244 canonicals, 111 compared, 108 agree, the same three equivalent
static folds, 131 declined, two failed to fold. Gilt-Leaf Ambush is among the declines;
its three scenario tests provide the behavioral evidence.

## Existing repository drift

The initial repository-wide backlog implementation check reported 78 implemented-but-unchecked entries in Bloomburrow Commander and its deck lists. Lorwyn had no such drift before this unit. These unrelated files are untouched. All 14 card-count headers passed the count check.


## Hunt Down

Merged current main without conflicts and rechecked all three source-reference regressions.
Hunt Down uses the existing two-target `ForceBlock` effect. Its nine scenarios pass: ordinary
blocking, an animated Forest, flying and same-controller restrictions, either target dying before
resolution, either creature leaving and returning, and end-of-turn expiry. The tests reproduced
two shared engine bugs before their fixes: printed-type checks rejected animated lands, and an
old blocking requirement followed a returned attacker. The executor now reads projected creature
types; battlefield cleanup discards the requirement when its named attacker leaves.

The full `just test` gate passed (4m 53s, 106 tasks). Snapshot regeneration passed (19s, 29 tasks);
only Hunt Down was added, with all 244 prior Lorwyn canonical entries unchanged. Fresh compiled
fields, artwork HTTP 200, and canonical-printing checks pass. Assay reports 245 canonicals,
111 compared, 108 agreements, the same three equivalent folds, 132 declines and two failed folds.
Hunt Down is declined; its scenarios prove behavior. Source and checklist agree at 251 / 286.

The existing cast-time battlefield targeting flow selects the blocker and attacker separately,
and the existing server effect indicator names the creature to be blocked. Combat validation and
turn cleanup reuse the current engine paths; no SDK type or client interaction was added. No
manual playthrough or end-to-end UI run was performed. Champion and the other completion work
remain outstanding; the set stays incomplete and the PR stays draft.

## Turtleshell Changeling and reusable switching

Turtleshell Changeling adds the missing `Effects.SwitchPowerToughness(target, duration)` atom.
It uses the existing floating-effect switch layer, source-instance guard, duration cleanup,
projected stats, and `StatsModifiedEvent` client mapping. The existing ability menu displays the
server's description; no new decision or client rule is introduced.

Seven card scenarios pass: expiry, double switching, boosts before/after switching, lethal
marked damage, and blinking before/after resolution. The blink tests cast the creature through
the real entry path so its battlefield identity is present. Three shared engine scenarios cover
other targets, permanent duration, noncreature animation, event emission, and immutable input.
The SDK round-trip and two mtgish emitter scenarios pass. The generator now maps `SwitchPT`;
67 fixture comment changes give previously empty scaffold diagnostics a readable fallback.
They change no generated code.

The full `just test` gate passed (3m 22s, 106 tasks). The card snapshot adds only Turtleshell;
all existing entries are unchanged. Fresh Scryfall metadata, image HTTP 200, and the canonical
LRW printing check passed. A manual setup is in
`manual-scenarios/cards/t/turtleshell-changeling.json`; it has not been played through in the UI.
No manual playthrough, UX session, or e2e test was performed. Full set verification remains owed.
