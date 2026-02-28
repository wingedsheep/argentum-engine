package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Final Punishment
 * {3}{B}{B}
 * Sorcery
 * Target player loses life equal to the damage already dealt to that player this turn.
 */
val FinalPunishment = card("Final Punishment") {
    manaCost = "{3}{B}{B}"
    typeLine = "Sorcery"
    oracleText = "Target player loses life equal to the damage already dealt to that player this turn."

    spell {
        val t = target("target", Targets.Player)
        effect = Effects.LoseLife(DynamicAmounts.damageDealtToTargetPlayerThisTurn(), t)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "67"
        artist = "Matt Thompson"
        flavorText = "The pain of a lifetime—every scrape, illness, and bruise—condensed into a single moment."
        imageUri = "https://cards.scryfall.io/normal/front/0/9/097dbfae-1a18-4c10-8d1f-b2c20971c75e.jpg?1562525364"
    }
}
