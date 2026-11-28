package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Harrowing Journey
 * {4}{B}
 * Sorcery
 *
 * Target player draws three cards and loses 3 life.
 */
val HarrowingJourney = card("Harrowing Journey") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target player draws three cards and loses 3 life."

    spell {
        val player = target(Targets.Player)
        effect = Effects.DrawCards(3, player) then Effects.LoseLife(3, player)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "67"
        artist = "James Paick"
        flavorText = "\"There are always fools who attempt to traverse Kruin Pass before sundown and every time they wind up in a race for their lives.\"\n—Old Rutstein"
        imageUri = "https://cards.scryfall.io/normal/front/9/c/9cf96a6c-8481-4954-b149-7153b80480be.jpg?1783940828"
    }
}
