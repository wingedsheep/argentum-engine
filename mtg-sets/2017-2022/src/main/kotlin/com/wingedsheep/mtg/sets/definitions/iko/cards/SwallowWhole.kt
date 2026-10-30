package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Swallow Whole
 * {W}
 * Sorcery
 * As an additional cost to cast this spell, tap an untapped creature you control.
 * Exile target tapped creature. Put a +1/+1 counter on the creature tapped to pay this spell's
 * additional cost.
 */
val SwallowWhole = card("Swallow Whole") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "As an additional cost to cast this spell, tap an untapped creature you control.\n" +
        "Exile target tapped creature. Put a +1/+1 counter on the creature tapped to pay this spell's additional cost."

    additionalCost(Costs.additional.TapPermanents(1))

    spell {
        val t = target(TargetFilter.TappedCreature)
        effect = Effects.Exile(t) then
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.TappedAsCost())
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "35"
        artist = "Svetlin Velinov"
        flavorText = "\"World's better with one fewer poacher in it.\"\n—Winota, heartbonder"
        imageUri = "https://cards.scryfall.io/normal/front/c/3/c3b9140b-848f-4886-8c28-eccd3829a607.jpg?1783931081"
    }
}
