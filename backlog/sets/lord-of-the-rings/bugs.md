# LTR bug list

Status legend: [ ] open · [repro] reproduced with a failing test (LtrBugTriageTest) · [fixed] · [?] not yet reproduced

## Original list

- [repro] **Wizard's Rockets** — no choosing of X. Activating `{X},{T},Sacrifice` returns failure; `{X}` in an activated/mana ability cost is unsupported (engine).
- [fixed] **Spiteful Banditry** — Treasure triggers multiple times. Was a per-creature `leavesBattlefield(binding=ANY)` trigger; simultaneous deaths each fired before the once-per-turn marker was set. Fixed by adding a batched, opponent-scoped `Triggers.OneOrMoreCreaturesAnOpponentControlsDie` (the die-batch trigger now honors the filter's controller predicate, mirroring the enter-batch). Covered by SpitefulBanditryScenarioTest.
- [?] **Lobelia Sackville-Baggins** — doesn't trigger. Existing scenario test passes; need a tighter repro (does it fail to go on the stack, or trigger-then-fizzle? how/whose-turn was the opponent's creature put into the graveyard "this turn"?).
- [repro] **Pippin's Bravery** — "may sacrifice a Food" / don't need Food. With no Food the target gets neither +4/+4 nor +2/+2 (stays at base); the `ifNotPaid` (+2/+2) branch doesn't run when the Sacrifice cost is unpayable (card/engine).
- [repro] **Ring-bearer preserved between zones** — `RingBearerComponent` persists across exile→return; the returned permanent is a new object and must not stay the Ring-bearer (CR 701.54) (engine).
- [repro] **Meneldor — Bilbo's Ring still attached** — Equipment is not detached when the equipped creature leaves the battlefield (CR 301.5/701.3) (engine).
- [repro] **Bilbo's Ring — attacks alone.** Corrected repro: when the equipped creature does **not** attack alone (attacks alongside others), the controller still draws. The "attacks alone" trigger fires when it shouldn't (engine/trigger predicate).

## Added later

- [root-caused] **Glorious Gale** — "the Ring tempts you" not working when countering a legendary creature spell. Likely cause: `ConditionalEffect(TargetMatchesFilter(Any.legendary(), 0), TheRingTemptsYou()).then(CounterSpell())` — the legendary check against the *spell on the stack* (target 0) returns false, so the tempt branch is skipped. Repro pending (the simple test driver can't target a spell-on-stack like a counterspell needs).
- [root-caused] **Elrond, Master of Healing** — draws a card when a creature you control *without* a +1/+1 counter becomes the target of an opponent's spell/ability. The trigger `CreatureYouControlBecomesTargetByOpponent(Creature.withCounter(PLUS_ONE_PLUS_ONE))` isn't applying its `withCounter` filter. Repro pending (needs an opponent spell/ability targeting your creature).
- [fixed] **Frodo Baggins** — power/toughness was 1/1, should be 1/3. Fixed in the LTR field-verification pass (commit "Verify LTR card fields against Scryfall; fix P/T and text divergences").
- [repro] **Friendly Rivalry** — can choose the same creature for both "creature you control" targets; casting with target0==target1 is accepted (should be illegal). The `.other()` on the second target excludes the *source*, not target 0.
