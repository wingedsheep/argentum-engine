package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Screaming Phantom — {2}{B}
 * Creature — Spirit
 * 2/2
 * Flying
 * Whenever this creature attacks, mill a card.
 */
val ScreamingPhantom = card("Screaming Phantom") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Spirit"
    oracleText = "Flying\nWhenever this creature attacks, mill a card. (Put the top card of your library into your graveyard.)"
    power = 2
    toughness = 2

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = Patterns.Library.mill(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "118"
        artist = "Halil Ural"
        flavorText = "After a slow death in darkness, she flies into a rage at the slightest glimpse of light."
        imageUri = "https://cards.scryfall.io/normal/front/b/c/bc2f3efb-1526-48c0-842a-374af63ea467.jpg?1782694518"
    }
}
