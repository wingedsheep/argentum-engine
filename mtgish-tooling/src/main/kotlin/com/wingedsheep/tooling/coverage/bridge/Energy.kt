package com.wingedsheep.tooling.coverage.bridge

/**
 * Energy counters (Kaladesh block onward, CR 107.14, CR 122.1): a resource tracked per *player*,
 * not per permanent — reusing the same `CountersComponent` poison counters already live on.
 *
 * Three IR tags, mapped here. The bridge only answers "can Argentum express this?" — whether the
 * *emitter* can safely auto-render a given card is a separate question. `PayAnyAmountOfEnergy` /
 * `TheAmountOfEnergyPaidThisWay` are exactly the "value chosen mid-resolution, inherited into a
 * later effect" shape this module's own README flags as an open gap (see
 * "Creator's note: extra costs & chosen / inherited values") — the emitter should keep declining
 * to SCAFFOLD for cards using them rather than guess at wiring the `storeAmountAs` /
 * `VariableReference` pipeline variable name, even though the capability itself is real and tested
 * (`GalvanicDischargeScenarioTest`).
 */
fun BridgeBuilder.energy() {
    // `_Action: GetEnergy` — "You get {E}...". No distinct leaf: Effects.GetEnergy is DSL sugar over
    // the existing AddCounters (Counters.ENERGY, count, target), the same way poison-to-a-player
    // already works (AddCountersExecutor's PlayerRef branch, Virulent Silencer).
    composed(
        "GetEnergy",
        "\"You get N energy counters\" (CR 107.14) -> Effects.GetEnergy(amount) = AddCounters(Counters.ENERGY, amount, Controller)",
        composes = listOf("AddCounters")
    )
    // `_Action: PayAnyAmountOfEnergy` — "you may pay any amount of {E}". A genuine new leaf effect
    // (PayCountersEffect, @SerialName "PayCounters"): a single ChooseNumberDecision (0..current
    // energy), removes that many, and stores the paid amount in the pipeline for a later action to
    // read. Paying 0 is always legal (2024-06-07 ruling on Galvanic Discharge).
    effect(
        "PayAnyAmountOfEnergy",
        "PayCounters",
        "player pays any amount of energy they have (CR 107.14) -> Effects.PayCounters(Counters.ENERGY, storeAmountAs = \"...\")"
    )
    // `_GameNumber: TheAmountOfEnergyPaidThisWay` — the amount just paid via PayAnyAmountOfEnergy,
    // read by a later action (Galvanic Discharge's SpellDealsDamage). Argentum expresses this as
    // DynamicAmount.VariableReference(storeAmountAs) reading the same pipeline slot PayCounters just
    // wrote — but the emitter has no `declare`-style bookkeeping to thread one auto-picked
    // storeAmountAs name from the PayAnyAmountOfEnergy node to this one, so cards using it still
    // decline to SCAFFOLD (see the module's "chosen / inherited values" note) even though the
    // capability itself is real.
    composed(
        "TheAmountOfEnergyPaidThisWay",
        "the amount paid by a prior PayAnyAmountOfEnergy -> DynamicAmount.VariableReference(the matching storeAmountAs)",
        composes = listOf("VariableReference")
    )
    // `_Cost: PayEnergy` — a *fixed* amount, nested inside a `MayCost` envelope: "you may pay
    // {E}{E}{E}. When you do, ..." (Guide of Souls, MH3). Distinct from PayAnyAmountOfEnergy (a
    // chosen amount 0..current, no separate reflexive trigger) — here the amount is fixed and the
    // payoff is a genuine CR 603.12 reflexive trigger with its own targets, not a same-ability "if
    // you do" continuation. A new leaf effect (PayFixedCountersEffect, @SerialName
    // "PayFixedCounters"): fails outright rather than clamping when the payer has fewer than the
    // named amount, so `ReflexiveTriggerEffectExecutor.isActionFeasible` can gate the "may pay"
    // prompt itself on affordability (mirrors how it already gates SacrificeEffect) rather than
    // offering an impossible yes. Tested via GuideOfSoulsScenarioTest.
    effect(
        "PayEnergy",
        "PayFixedCounters",
        "player pays an exact amount of energy they have (CR 107.14) -> Effects.PayFixedCounters(Counters.ENERGY, amount)"
    )
}
