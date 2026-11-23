package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.plus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Predator's Rapport
 * {2}{G}
 * Instant
 * Choose target creature you control. You gain life equal to that creature's power plus its toughness.
 *
 * Sheltering Word's shape: the amount is read at resolution off the spell's own target, power and
 * toughness summed with the `DynamicAmount` `+` operator.
 */
val PredatorsRapport = card("Predator's Rapport") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Choose target creature you control. You gain life equal to that creature's power " +
        "plus its toughness."

    spell {
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.GainLife(DynamicAmounts.powerOf(creature) + DynamicAmounts.toughnessOf(creature))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "129"
        artist = "Matt Stewart"
        flavorText = "\"Other guilds say the Gruul are savages, no better than the beasts we live with. " +
            "I say we've found friends who won't stab us in the back.\"\n—Domri Rade"
        imageUri = "https://cards.scryfall.io/normal/front/4/7/47324ab7-df78-4859-be0b-2eef5d4f8082.jpg?1783940116"
    }
}
