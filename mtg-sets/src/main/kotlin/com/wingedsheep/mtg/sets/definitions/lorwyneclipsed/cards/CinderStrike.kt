package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect

/**
 * Cinder Strike
 * {R}
 * Sorcery
 *
 * As an additional cost to cast this spell, you may blight 1.
 * (You may put a -1/-1 counter on a creature you control.)
 * Cinder Strike deals 2 damage to target creature. It deals 4 damage to that creature
 * instead if this spell's additional cost was paid.
 */
val CinderStrike = card("Cinder Strike") {
    manaCost = "{R}"
    typeLine = "Sorcery"
    oracleText = "As an additional cost to cast this spell, you may blight 1. " +
        "(You may put a -1/-1 counter on a creature you control.)\n" +
        "Cinder Strike deals 2 damage to target creature. It deals 4 damage to that creature " +
        "instead if this spell's additional cost was paid."

    additionalCost(AdditionalCost.BlightOrPay(blightAmount = 1, alternativeManaCost = ""))

    spell {
        val creature = target("creature", Targets.Creature)
        effect = ConditionalEffect(
            condition = Conditions.BlightWasPaid,
            effect = Effects.DealDamage(4, creature),
            elseEffect = Effects.DealDamage(2, creature)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "131"
        artist = "Joshua Raphael"
        flavorText = "\"If I am to gutter out, at least you will burn.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/f/6fb6faa4-236c-4cae-9140-0981c44d2392.jpg?1767957230"
    }
}
