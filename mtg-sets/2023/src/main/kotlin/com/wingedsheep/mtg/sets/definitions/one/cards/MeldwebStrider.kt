package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Meldweb Strider
 * {4}{U}
 * Artifact — Vehicle
 * 5/5
 *
 * Vigilance
 * This Vehicle enters with an oil counter on it.
 * Remove an oil counter from this Vehicle: It becomes an artifact creature until end of turn.
 * Crew 3
 *
 * The oil-counter ability is a self-crew: like crew, it animates the Vehicle with its printed 5/5
 * for the turn (it is already an artifact, so only CREATURE is added). Printed vigilance rides along.
 */
val MeldwebStrider = card("Meldweb Strider") {
    manaCost = "{4}{U}"
    typeLine = "Artifact — Vehicle"
    power = 5
    toughness = 5
    oracleText = "Vigilance\n" +
        "This Vehicle enters with an oil counter on it.\n" +
        "Remove an oil counter from this Vehicle: It becomes an artifact creature until end of turn.\n" +
        "Crew 3 (Tap any number of creatures you control with total power 3 or more: This Vehicle becomes an artifact creature until end of turn.)"

    keywords(Keyword.VIGILANCE)

    replacementEffect(EntersWithCounters(counterType = CounterType.OIL, count = 1, selfOnly = true))

    activatedAbility {
        cost = Costs.RemoveCounterFromSelf(CounterType.OIL, 1)
        effect = Effects.BecomeCreature(EffectTarget.Self, power = 5, toughness = 5)
        description = "This Vehicle becomes an artifact creature until end of turn."
    }

    keywordAbility(KeywordAbility.crew(3))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "60"
        artist = "Julian Kok Joon Wen"
        imageUri = "https://cards.scryfall.io/normal/front/c/5/c5efd9b5-05e5-440f-b28c-658e461cf644.jpg?1783918061"
    }
}
