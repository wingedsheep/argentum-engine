package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Lethal Sting
 * {2}{B}
 * Sorcery
 *
 * As an additional cost to cast this spell, put a -1/-1 counter on a creature you control.
 * Destroy target creature.
 */
val LethalSting = card("Lethal Sting") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "As an additional cost to cast this spell, put a -1/-1 counter on a creature you control.\n" +
        "Destroy target creature."

    additionalCost(Costs.additional.PutCountersOnPermanent(CounterType.MINUS_ONE_MINUS_ONE))

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.Destroy(creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "67"
        artist = "Randy Vargas"
        flavorText = "Initiates once believed defeat was the worst fate possible—a sentiment they quickly abandoned."
        imageUri = "https://cards.scryfall.io/normal/front/e/a/eaded6bf-2db7-4b1d-93cc-4b7b571cd2de.jpg?1783936039"
    }
}
