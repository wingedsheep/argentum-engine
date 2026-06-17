# LTR bug list

Status legend: [ ] open · [repro] reproduced with a failing test (LtrBugTriageTest) · [fixed] · [?] not yet reproduced

## Original list

- [repro] **Wizard's Rockets** — no choosing of X. Activating `{X},{T},Sacrifice` returns failure; `{X}` in an activated/mana ability cost is unsupported (engine).
- [fixed] **Spiteful Banditry** — Treasure triggers multiple times. Was a per-creature `leavesBattlefield(binding=ANY)` trigger; simultaneous deaths each fired before the once-per-turn marker was set. Fixed by adding a batched, opponent-scoped `Triggers.OneOrMoreCreaturesAnOpponentControlsDie` (the die-batch trigger now honors the filter's controller predicate, mirroring the enter-batch). Covered by SpitefulBanditryScenarioTest.
- [?] **Lobelia Sackville-Baggins** — doesn't trigger. Existing scenario test passes; need a tighter repro (does it fail to go on the stack, or trigger-then-fizzle? how/whose-turn was the opponent's creature put into the graveyard "this turn"?).
- [root-caused, open] **Pippin's Bravery** — the targeted creature gets neither +4/+4 nor +2/+2. Two layers: (1) `GatedEffectExecutor.canAfford` fails open for a Sacrifice cost, so the unpayable "no Food" case never falls to the otherwise-branch; (2) deeper — even *with* a Food and accepting the sacrifice, the +4/+4 never applies, because the `Effects.ModifyStats(.., creature)` uses a `BoundVariable` target whose `namedTargets` binding does not reach the gated then/otherwise branch (a continuation/targets-propagation fizzle in the `OptionalCostEffect` path). Needs the gated-effect target-propagation fix, not just the canAfford fix. (engine)
- [fixed] **Ring-bearer preserved between zones** — `RingBearerComponent` persisted across exile→return. Fixed by stripping it in `ZoneMovementUtils.stripBattlefieldComponents` (CR 701.54e / 400.7). Covered by RingBearerAndEquipmentZoneChangeScenarioTest.
- [fixed] **Meneldor — Bilbo's Ring still attached** — Equipment attached to a creature that left the battlefield kept its `AttachedToComponent`. Fixed: `ZoneTransitionService.moveToZone` now detaches permanents attached to a leaving permanent (`ZoneMovementUtils.detachPermanentsAttachedTo`, CR 704.5q). Covered by RingBearerAndEquipmentZoneChangeScenarioTest.
- [fixed] **Bilbo's Ring — attacks alone.** The "attacks alone" trigger fired even when the equipped creature attacked alongside others. `AttachmentTriggerDetector` (which handles ATTACHED-binding triggers) fired on bare membership in the attacker set without applying the trigger's `requires` predicates (`AttackPredicate.Alone`). Fixed to apply `requires`, mirroring the main loop. Covered by BilbosRingAttacksAloneScenarioTest; empty-`requires` attached-attack triggers (Stormbeacon Blade, Thunder Lasso) still fire.

## Added later

- [not-a-bug] **Glorious Gale** — verified working: the existing GloriousGaleScenarioTest pins both "counters a legendary creature spell and the Ring tempts you" (temptCount 1) and the nonlegendary case (0). The legendary check on the countered spell-on-stack resolves correctly; no change needed.
- [fixed] **Elrond, Master of Healing** — drew a card even when the targeted creature had no +1/+1 counter. `TriggerMatcher.matchesBecomesTargetTrigger` never evaluated the target filter's `statePredicates`; fixed to apply them. Covered by ElrondMasterOfHealingTargetTriggerTest.
- [fixed] **Frodo Baggins** — power/toughness was 1/1, should be 1/3. Fixed in the LTR field-verification pass (commit "Verify LTR card fields against Scryfall; fix P/T and text divergences").
- [repro] **Friendly Rivalry** — can choose the same creature for both "creature you control" targets; casting with target0==target1 is accepted (should be illegal). The `.other()` on the second target excludes the *source*, not target 0.
