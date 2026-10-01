package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Bladed Ambassador
 * {1}{W}
 * Creature — Phyrexian Soldier
 * 3/1
 *
 * This creature enters with an oil counter on it.
 * {1}, Remove an oil counter from this creature: This creature gains indestructible until end of turn.
 */
val BladedAmbassador = card("Bladed Ambassador") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Phyrexian Soldier"
    power = 3
    toughness = 1
    oracleText = "This creature enters with an oil counter on it.\n" +
        "{1}, Remove an oil counter from this creature: This creature gains indestructible until end of turn."

    replacementEffect(EntersWithCounters(counterType = CounterType.OIL, count = 1, selfOnly = true))

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.RemoveCounterFromSelf(CounterType.OIL, 1))
        effect = Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, EffectTarget.Self)
        description = "{1}, Remove an oil counter from this creature: This creature gains indestructible until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "5"
        artist = "Nino Vecia"
        flavorText = "\"Let those thanebound dissenters prattle about their dead god. I serve the Mother of Machines, " +
            "and in her name I carve out their heresy.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/b/0bd8d2b8-4caf-4349-9538-0dfbebf0ce1b.jpg?1783918086"
    }
}
