package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Radiant Flames
 * {2}{R}
 * Sorcery
 * Converge — Radiant Flames deals X damage to each creature, where X is the number of colors of mana
 * spent to cast this spell.
 *
 * Converge: X = [DynamicAmounts.colorsOfManaSpent], read off the resolving spell's recorded payment
 * (the Painful Truths shape), dealt to each creature the Earthquake way. A copy, or a free cast, has
 * X = 0.
 */
val RadiantFlames = card("Radiant Flames") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Converge — Radiant Flames deals X damage to each creature, where X is the number of " +
        "colors of mana spent to cast this spell."

    spell {
        effect = Effects.ForEachInGroup(
            GroupFilter.AllCreatures,
            Effects.DealDamage(DynamicAmounts.colorsOfManaSpent(), EffectTarget.IterationEntity)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "151"
        artist = "Slawomir Maniak"
        flavorText = "The fires of Zendikar are always hungry, and there is much on which they can feed."
        imageUri = "https://cards.scryfall.io/normal/front/7/0/70f4fe69-c541-4320-9074-9c6a3bc70ea3.jpg?1783938193"
    }
}
