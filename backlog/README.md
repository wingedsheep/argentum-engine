# Backlog

Open plans, proposals and point-in-time reports. A doc moves to [`archived/`](archived/) once its
work has shipped, been dropped or been superseded. Archived docs stay as design history, and code
comments that cite them point at their `archived/` path.

Set backlogs live in [`sets/`](sets/). A set folder leaves `sets/` only through the `verify-set`
skill, which proves the set complete and then archives it. A set reading N/N in its `cards.md` isn't
archived until that has run.

## Engine and SDK

- [`sdk-consolidation-from-usage-data.md`](sdk-consolidation-from-usage-data.md): type-graph and
  card-usage findings plus code research: why the long tail grows, five consolidation proposals, and bugs found along the way.
- [`engine-sdk-architecture-review.md`](engine-sdk-architecture-review.md): the 2026-09 review; §5
  largely shipped, §6 (generation) open.
- [`target-union-with-arms.md`](target-union-with-arms.md): a composable mixed target.
- [`cast-time-choices-and-inherited-x.md`](cast-time-choices-and-inherited-x.md): phases 1–2 done,
  `declare {}` DSL open.
- [`paycost-payment-unification.md`](paycost-payment-unification.md): option C in progress.
- [`number-explosion-safety.md`](number-explosion-safety.md): options A and B done, C open.
- [`phase-rs-lessons.md`](phase-rs-lessons.md): lessons 3 and 4 open.
- [`testing-strategy.md`](testing-strategy.md): what to test where; optional phase 4 open.

## Coverage and tooling

- [`assay-engine-gap-analysis.md`](assay-engine-gap-analysis.md): Assay vs engine vocabulary
  snapshot.
- [`token-art-gaps.md`](token-art-gaps.md): tokens without set-scoped art.
- [`forge-parity-harness.md`](forge-parity-harness.md): cross-engine differential tester.
- [`magezero-coverage.md`](magezero-coverage.md): cards MageZero needs.
- [`multi-printing-system.md`](multi-printing-system.md): printing picker UX and Scryfall catalogue
  open.

## AI

- [`engine-ai-improvement.md`](engine-ai-improvement.md): phase 9 underway.
- [`ecl-ai-training-plan.md`](ecl-ai-training-plan.md): Lorwyn Eclipsed training and promotion.

## Formats and modes

- [`cube-draft-format.md`](cube-draft-format.md): the next pick.
- [`commander-format.md`](commander-format.md): phase 4 (partner, background, companion) open.
- [`brawl-draft-format.md`](brawl-draft-format.md)
- [`emperor-format.md`](emperor-format.md)
- [`multiplayer.md`](multiplayer.md): phase 1.4 partly open.
- [`multiplayer-ui-followups.md`](multiplayer-ui-followups.md)

## Site and accounts

- [`menu-lobby-restructure-and-help.md`](menu-lobby-restructure-and-help.md): phases 5, 6b and 9
  open.
- [`seasonal-binder-league.md`](seasonal-binder-league.md)
- [`league-season-system.md`](league-season-system.md): its auth phase is superseded by magic-link
  accounts ([`docs/accounts-and-persistence.md`](../docs/accounts-and-persistence.md)).
