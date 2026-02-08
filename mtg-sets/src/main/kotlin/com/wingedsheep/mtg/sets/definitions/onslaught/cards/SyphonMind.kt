package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EachOpponentDiscardsEffect

/**
 * Syphon Mind
 * {3}{B}
 * Sorcery
 * Each other player discards a card. You draw a card for each card discarded this way.
 */
val SyphonMind = card("Syphon Mind") {
    manaCost = "{3}{B}"
    typeLine = "Sorcery"

    spell {
        effect = EachOpponentDiscardsEffect(count = 1, controllerDrawsPerDiscard = 1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "175"
        artist = "Jeff Easley"
        flavorText = "When tempers run high, it's easy to lose your head."
        imageUri = "https://cards.scryfall.io/normal/front/0/b/0b0d8543-78c9-4d7f-b45e-44ecf023d276.jpg?1562897508"
    }
}
