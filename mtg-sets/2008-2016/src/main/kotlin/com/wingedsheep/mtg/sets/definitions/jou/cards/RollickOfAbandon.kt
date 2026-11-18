package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Rollick of Abandon
 * {3}{R}{R}
 * Sorcery
 *
 * All creatures get +2/-2 until end of turn.
 */
val RollickOfAbandon = card("Rollick of Abandon") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "All creatures get +2/-2 until end of turn."

    spell {
        effect = Effects.ForEachInGroup(
            GroupFilter.AllCreatures,
            Effects.ModifyStats(2, -2, EffectTarget.IterationEntity),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "108"
        artist = "Mathias Kollros"
        flavorText = "\"Embrace every moment, for it could be your last.\"\n—Xenagos, god of revels"
        imageUri = "https://cards.scryfall.io/normal/front/3/f/3f1a80c4-8119-437d-bf5b-549c5679c90a.jpg?1783939420"
    }
}
