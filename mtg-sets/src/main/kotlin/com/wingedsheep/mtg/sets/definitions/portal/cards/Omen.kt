package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.OmenEffect

/**
 * Omen
 * {1}{U}
 * Sorcery
 * Look at the top three cards of your library, then put them back in any order.
 * You may shuffle. Draw a card.
 */
val Omen = card("Omen") {
    manaCost = "{1}{U}"
    typeLine = "Sorcery"

    spell {
        effect = OmenEffect(
            lookAtCount = 3,
            mayShuffle = true,
            drawAfter = 1
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "62"
        artist = "Eric Peterson"
        imageUri = "https://cards.scryfall.io/normal/front/c/a/ca5c8e94-5b3a-4bc1-a91b-6bec6c62a3a7.jpg"
    }
}
