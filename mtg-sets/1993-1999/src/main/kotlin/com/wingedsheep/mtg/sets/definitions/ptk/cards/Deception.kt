package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Deception
 * {2}{B}
 * Sorcery
 * Target opponent discards two cards.
 */
val Deception = card("Deception") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target opponent discards two cards."

    spell {
        val opponent = target(Targets.Opponent)
        effect = Patterns.Hand.discardCards(2, opponent)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "73"
        artist = "Wang Feng"
        flavorText = "Believing they wrote the forged letter that had been stolen from Zhou Yu, Cao Cao rashly executed his two best admirals for treason."
        imageUri = "https://cards.scryfall.io/normal/front/b/d/bdb63768-2d8a-4a74-a312-b981dd462cdc.jpg?1783946116"
    }
}
