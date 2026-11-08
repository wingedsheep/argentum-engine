package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect

/**
 * Lathnu Hellion — Kaladesh #121
 * {2}{R} · Creature — Hellion · 4/4
 *
 * Haste
 * When this creature enters, you get {E}{E} (two energy counters).
 * At the beginning of your end step, sacrifice this creature unless you pay {E}{E}.
 */
val LathnuHellion = card("Lathnu Hellion") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Hellion"
    power = 4
    toughness = 4
    oracleText = "Haste\n" +
        "When this creature enters, you get {E}{E} (two energy counters).\n" +
        "At the beginning of your end step, sacrifice this creature unless you pay {E}{E}."

    keywords(Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GetEnergy(2)
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        effect = Effects.PayOrSuffer(
            cost = Costs.pay.PayPlayerCounters(CounterType.ENERGY, 2),
            suffer = SacrificeSelfEffect,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "121"
        artist = "Lars Grant-West"
        imageUri = "https://cards.scryfall.io/normal/front/1/4/14050179-84af-46f6-89be-338f8e7131cc.jpg?1783937192"
    }
}
