package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Electrozoa
 * {2}{U}
 * Creature — Jellyfish
 * 3/1
 *
 * Flash
 * Flying
 * When this creature enters, you get {E}{E} (two energy counters).
 * At the beginning of your first main phase, tap this creature unless you pay {E}.
 *
 * "Your first main phase" is the precombat main phase — every additional main phase is a
 * postcombat one (CR 505.1a). The upkeep tax is a resolution-time [Effects.PayOrSuffer] over one
 * energy; with no energy to pay, the creature is simply tapped.
 */
val Electrozoa = card("Electrozoa") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Jellyfish"
    power = 3
    toughness = 1
    oracleText = "Flash\nFlying\n" +
        "When this creature enters, you get {E}{E} (two energy counters).\n" +
        "At the beginning of your first main phase, tap this creature unless you pay {E}."

    keywords(Keyword.FLASH, Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GetEnergy(2)
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.PRECOMBAT_MAIN)
        effect = Effects.PayOrSuffer(
            cost = Costs.pay.PayPlayerCounters(CounterType.ENERGY, 1),
            suffer = Effects.Tap(EffectTarget.Self),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "60"
        artist = "Steve Ellis"
        imageUri = "https://cards.scryfall.io/normal/front/0/f/0f92dce7-45d4-4d9e-abfc-e4bfce9562c9.jpg?1783911291"
    }
}
