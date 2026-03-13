package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Artificer's Assistant
 * {U}
 * Creature — Bird
 * 1/1
 * Flying
 * Whenever you cast a historic spell, scry 1.
 * (Artifacts, legendaries, and Sagas are historic.)
 */
val ArtificersAssistant = card("Artificer's Assistant") {
    manaCost = "{U}"
    typeLine = "Creature — Bird"
    power = 1
    toughness = 1
    oracleText = "Flying\nWhenever you cast a historic spell, scry 1. (Artifacts, legendaries, and Sagas are historic.)"

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.YouCastHistoric
        effect = EffectPatterns.scry(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "44"
        artist = "Chris Seaman"
        imageUri = "https://cards.scryfall.io/normal/front/d/e/deadf867-b999-49b2-88d8-91da975a3cc5.jpg?1562744145"
    }
}
