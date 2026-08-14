# u10 — Equip worthy (Mjölnir, Hammer of Thor)

Branch `loop-msh-u10`, off `origin/main`. PR body: `build/pr/loop-msh-u10-body.md`.

## Cards

- **Mjölnir, Hammer of Thor** (MSH #146) — Legendary Equipment. ETB 4 damage to up to one target
  creature; `DoubleDamage(source = SourceFilter.EquippedCreature)`; `equipAbility(quality = "worthy")`;
  from-hand `Costs.DiscardSelf` ability sweeping 2 damage to each creature via
  `Effects.ForEachInGroup`. Composes existing primitives except for the two SDK additions below.

## SDK

1. `equipAbility(cost, genericCostReduction, quality, targetFilter)` — CR 702.6c "Equip [quality]".
   `quality` is wording (becomes the target requirement's id/label); `targetFilter` is the rules half
   and must stay controller-scoped. No new target machinery — it is an ordinary `TargetFilter` on an
   ordinary `TargetRequirement`.
2. `SourceFilter.EquippedCreature` — the brief's "one-line mirror", verified: `RecipientFilter` already
   had the pair in a shared branch, `SourceFilter` had only `EnchantedCreature`.
   `DamageUtils.damageSourceMatches` now handles both in one branch. That matcher is shared by
   prevention/doubling/modification and by combat + noncombat, so nothing else needed touching.

## Convergence (deliberate, beyond the one card)

Five printed "Equip [quality]" cards were hand-rolling the shape as bare `activatedAbility { }` blocks
**without `isEquipAbility`**: Blackblade Reforged, Bilbo's Ring, Dúnedain Blade, Ghostfire Blade,
Pirate Hat. Converged onto the facade. This fixes a real pre-existing bug (Forge Anew / Eowyn /
instant-speed-equip all skipped those abilities) and is the reason five set snapshots move, not one.
Non-mana equip costs (Dissection Tools et al.) stay hand-rolled — the facade parses a mana cost only,
and those five already set the flag.

## Tests

- `MjolnirHammerOfThorScenarioTest` (rules-engine) — one file, one card.
- `EquipQualityVariantTest` (mtg-sets) — mechanic-level, catalog-wide invariants.

## Gate

Per-module (shared box, ~2.5 GiB free — a full `just test` has been OOM-killing this week):

- `scripts/gradle-locked :mtg-sdk:test :mtg-sets:test :rules-engine:test` — **11095 PASSED, 0 FAILED**,
  BUILD SUCCESSFUL, exit 0. Log: `build/pr/loop-msh-u10-gate.log`.
- `scripts/gradle-locked :ai:test :mtgish-tooling:test :game-server:test` — **1046 PASSED, 0 FAILED**,
  BUILD SUCCESSFUL, exit 0. Log: `build/pr/loop-msh-u10-gate2.log`.
- `just rebless-cards` — BUILD SUCCESSFUL. **Five snapshots move, not one**: MSH (the new card) plus
  DOM/KTK/LCI/LTR (the convergence: `descriptionOverride` → `isEquipAbility`, Bilbo's Ring's label, and
  a new `equipCost "{7}"` on Blackblade Reforged which previously had none). No other card moved.
- `just check-card-printing "Mjölnir, Hammer of Thor"` — ok, canonical in the earliest real printing.
- `just fix-backlog` — headers in sync (239 → 240 hand-bumped).

Not done: no web-client playthrough, no e2e, no UX pass from either seat.

## Things I'm unsure about — reviewer, look here

- ~~**Menu text change on five existing cards.**~~ **Resolved in the review round.** Rather than
  choose between the printed wording and cost-reduction awareness, `ActivatedAbility.describeWithCost`
  now renders any `isEquipAbility` ability as its printed line — "Equip {3}" (CR 702.6a),
  "Equip Human {1}" (CR 702.6c), "Equip—Pay 3 life" for a non-mana cost — against the *effective*
  cost. That restores the printed text on all five and gets the discount-aware label too, which a
  static `descriptionOverride` could never do. The new `equipQuality` field carries the wording. Note
  this changes the menu text for *every* Equipment in the catalog, not just the converged ones.
- **Scope.** The convergence is wider than "the card it unblocks". I did it because the run's standing
  lesson is "don't add a parallel rail where one can be shared" and the parallel rail already existed
  with a latent bug. If the reviewer disagrees, the card + SDK halves stand alone without it.
- **`equipCost` metadata on a restricted equip.** `equipAbility` sets `equipCost` unconditionally, so a
  card with both a restricted and a plain equip ends up with the last one's cost (unchanged from
  before for all five converged cards, since the plain one is declared last in each). Mjölnir has only
  the restricted equip, so its `equipCost` is `{1}` — which is what the linter's
  "nothing can ever be attached" check wants, but it does mean `equipCost` is not always the
  unrestricted cost.
- **Three unconverged `SourceFilter` matchers** in `DamageUtils` (`DamageBonusComponent`, max-damage
  cap, `ReplaceDamageWithMill`) still hand-roll `Any`/`Matching`-only subsets. Pre-existing drift from
  the shared matcher the doc comment at `DamageUtils` ~L1390 describes; left alone as out of scope, but
  it is the kind of thing that bites later.
- I did **not** add a "worthy" keyword/reminder-text entry that the brief suggested. Scryfall says
  exactly one printed card uses the term, so it would be a one-card SDK concept; the card's own
  reminder text carries the definition.

## Review round 2 — fixes applied

- **`damageDoublersAffectingSource` was O(battlefield) per card.** It ran from
  `ClientStateTransformer.buildCardActiveEffects` for every card on every state push, making the view
  path quadratic. Now driven off the maintained `AttachmentsComponent` reverse index (the same one
  `TriggerAbilityResolver` and `DestroyAllEquipmentOnTargetExecutor` use), with an early return for
  the un-attached common case.
- **Two more cards were still hand-rolling "Equip [quality]"** — Thinking Cap ("Equip Detective {1}",
  MKM) and Wizard's Staff ("Equip Wizard {1}", HOB). Both *did* set `isEquipAbility`, so they weren't
  invisible to the engine like the original five, but both froze their cost in a
  `descriptionOverride`. Converged onto the facade. `EquipQualityVariantTest` found them — it is now
  three catalog-wide *properties* rather than a hardcoded card list, so it needs no edit per new
  Equipment and keys "is this restricted?" off the target filter rather than the prompt label.
- **The Irencrag's redundant `descriptionOverride = "Equip {3}"`** dropped — the renderer produces
  exactly that, and now does so against the discounted cost.
- **Playtest board fixed.** Frodo Baggins ({G}{W} Legendary Halfling Scout) is *worthy*, so it was a
  second legal equip target and covered no failing clause; swapped for Hercules, Prince of Power
  (mono-green legendary Hero). Thor is now the only worthy target and each other creature fails
  exactly one clause. (The `a516b0ec2d` commit message also names "Skyward Spider", which was never in
  the file — read the file, not that message.)
- **Doc-accuracy.** `isAttachmentScopedSource`'s KDoc no longer claims a type-level invariant that
  `SourceFilter.Self` would break; it states the catalog fact instead. The Aura half
  (`SourceFilter.EnchantedCreature` + `DoubleDamage`) is documented as unreachable from the current
  catalog — shared-matcher generality, not behaviour under test.
- **Client:** `double-damage` gained the tooltip border colour its badge style already had.
