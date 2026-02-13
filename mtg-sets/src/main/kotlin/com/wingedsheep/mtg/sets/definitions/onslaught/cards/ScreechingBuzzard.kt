package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EachOpponentDiscardsEffect

/**
 * Screeching Buzzard
 * {3}{B}
 * Creature — Bird
 * 2/2
 * Flying
 * When Screeching Buzzard dies, each opponent discards a card.
 */
val ScreechingBuzzard = card("Screeching Buzzard") {
    manaCost = "{3}{B}"
    typeLine = "Creature — Bird"
    power = 2
    toughness = 2
    oracleText = "Flying\nWhen Screeching Buzzard dies, each opponent discards a card."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.Dies
        effect = EachOpponentDiscardsEffect(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "165"
        artist = "Heather Hudson"
        imageUri = "https://cards.scryfall.io/normal/front/1/d/1d4b887a-d928-4f6c-aa37-a0b09e87b91e.jpg?1562901860"
    }
}
