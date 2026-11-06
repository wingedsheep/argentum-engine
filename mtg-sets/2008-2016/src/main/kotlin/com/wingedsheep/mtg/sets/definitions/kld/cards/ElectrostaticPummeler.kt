package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Electrostatic Pummeler — Kaladesh #210
 * {3} · Artifact Creature — Construct · 1/1
 *
 * When this creature enters, you get {E}{E}{E} (three energy counters).
 * Pay {E}{E}{E}: This creature gets +X/+X until end of turn, where X is its power.
 *
 * Oracle errata: the printed ability read "+1/+1"; current Oracle doubles its power instead.
 * X is read once, as the ability resolves, and a negative power counts as 0 — hence
 * [DynamicAmounts.nonNegative] over the source's projected power.
 */
val ElectrostaticPummeler = card("Electrostatic Pummeler") {
    manaCost = "{3}"
    typeLine = "Artifact Creature — Construct"
    power = 1
    toughness = 1
    oracleText = "When this creature enters, you get {E}{E}{E} (three energy counters).\n" +
        "Pay {E}{E}{E}: This creature gets +X/+X until end of turn, where X is its power."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GetEnergy(3)
    }

    activatedAbility {
        cost = Costs.PayPlayerCounters(CounterType.ENERGY, 3)
        val x = DynamicAmounts.nonNegative(DynamicAmounts.sourcePower())
        effect = Effects.ModifyStats(x, x, EffectTarget.Self)
        description = "Pay {E}{E}{E}: This creature gets +X/+X until end of turn, where X is its power."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "210"
        artist = "Slawomir Maniak"
        imageUri = "https://cards.scryfall.io/normal/front/c/5/c565d11b-a57e-4de4-9d18-2be48a2ef742.jpg?1783937158"
        ruling("2016-09-20", "The value of X is determined as Electrostatic Pummeler's last ability resolves.")
        ruling("2017-07-14", "If this creature's power is negative as its ability resolves, X is considered to be 0.")
    }
}
