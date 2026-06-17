# LTR bug list

Status legend: [ ] open · [repro] reproduced with a failing test (LtrBugTriageTest) · [fixed] · [?] not yet reproduced

## Original list

- [repro] **Wizard's Rockets** — no choosing of X. Activating `{X},{T},Sacrifice` returns failure; `{X}` in an activated/mana ability cost is unsupported (engine).
- [fixed] **Spiteful Banditry** — Treasure triggers multiple times. Was a per-creature `leavesBattlefield(binding=ANY)` trigger; simultaneous deaths each fired before the once-per-turn marker was set. Fixed by adding a batched, opponent-scoped `Triggers.OneOrMoreCreaturesAnOpponentControlsDie` (the die-batch trigger now honors the filter's controller predicate, mirroring the enter-batch). Covered by SpitefulBanditryScenarioTest.
- [?] **Lobelia Sackville-Baggins** — doesn't trigger. Existing scenario test passes; need a tighter repro (does it fail to go on the stack, or trigger-then-fizzle? how/whose-turn was the opponent's creature put into the graveyard "this turn"?).
- [repro] **Pippin's Bravery** — "may sacrifice a Food" / don't need Food. With no Food the target gets neither +4/+4 nor +2/+2 (stays at base); the `ifNotPaid` (+2/+2) branch doesn't run when the Sacrifice cost is unpayable (card/engine).
- [fixed] **Ring-bearer preserved between zones** — `RingBearerComponent` persisted across exile→return. Fixed by stripping it in `ZoneMovementUtils.stripBattlefieldComponents` (CR 701.54e / 400.7). Covered by RingBearerAndEquipmentZoneChangeScenarioTest.
- [fixed] **Meneldor — Bilbo's Ring still attached** — Equipment attached to a creature that left the battlefield kept its `AttachedToComponent`. Fixed: `ZoneTransitionService.moveToZone` now detaches permanents attached to a leaving permanent (`ZoneMovementUtils.detachPermanentsAttachedTo`, CR 704.5q). Covered by RingBearerAndEquipmentZoneChangeScenarioTest.
- [fixed] **Bilbo's Ring — attacks alone.** The "attacks alone" trigger fired even when the equipped creature attacked alongside others. `AttachmentTriggerDetector` (which handles ATTACHED-binding triggers) fired on bare membership in the attacker set without applying the trigger's `requires` predicates (`AttackPredicate.Alone`). Fixed to apply `requires`, mirroring the main loop. Covered by BilbosRingAttacksAloneScenarioTest; empty-`requires` attached-attack triggers (Stormbeacon Blade, Thunder Lasso) still fire.

## Added later

- [root-caused] **Glorious Gale** — "the Ring tempts you" not working when countering a legendary creature spell. Likely cause: `ConditionalEffect(TargetMatchesFilter(Any.legendary(), 0), TheRingTemptsYou()).then(CounterSpell())` — the legendary check against the *spell on the stack* (target 0) returns false, so the tempt branch is skipped. Repro pending (the simple test driver can't target a spell-on-stack like a counterspell needs).
- [root-caused] **Elrond, Master of Healing** — draws a card when a creature you control *without* a +1/+1 counter becomes the target of an opponent's spell/ability. The trigger `CreatureYouControlBecomesTargetByOpponent(Creature.withCounter(PLUS_ONE_PLUS_ONE))` isn't applying its `withCounter` filter. Repro pending (needs an opponent spell/ability targeting your creature).
- [fixed] **Frodo Baggins** — power/toughness was 1/1, should be 1/3. Fixed in the LTR field-verification pass (commit "Verify LTR card fields against Scryfall; fix P/T and text divergences").
- [repro] **Friendly Rivalry** — can choose the same creature for both "creature you control" targets; casting with target0==target1 is accepted (should be illegal). The `.other()` on the second target excludes the *source*, not target 0.
