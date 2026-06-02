package com.wingedsheep.mtg.sets.definitions.ecl.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.dsl.Costs

/**
 * Requiting Hex
 * {B}
 * Instant
 *
 * As an additional cost to cast this spell, you may blight 1.
 * (You may put a -1/-1 counter on a creature you control.)
 * Destroy target creature with mana value 2 or less. If this spell's
 * additional cost was paid, you gain 2 life.
 */
val RequitingHex = card("Requiting Hex") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "As an additional cost to cast this spell, you may blight 1. " +
        "(You may put a -1/-1 counter on a creature you control.)\n" +
        "Destroy target creature with mana value 2 or less. " +
        "If this spell's additional cost was paid, you gain 2 life."

    additionalCost(Costs.additional.BlightOrPay(blightAmount = 1, alternativeManaCost = ""))

    spell {
        val creature = target(
            "creature with mana value 2 or less",
            TargetObject(filter = TargetFilter(GameObjectFilter.Creature.manaValueAtMost(2)))
        )
        effect = Effects.Destroy(creature) then ConditionalEffect(
            condition = Conditions.BlightWasPaid,
            effect = Effects.GainLife(2)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "116"
        artist = "Randy Gallegos"
        imageUri = "https://cards.scryfall.io/normal/front/f/2/f21b0fb7-91b6-403f-a81a-562665961276.jpg?1767732705"
    }
}
