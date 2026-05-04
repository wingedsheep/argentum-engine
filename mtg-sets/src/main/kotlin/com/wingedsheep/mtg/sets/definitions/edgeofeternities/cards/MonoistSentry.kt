package com.wingedsheep.mtg.sets.definitions.edgeofeternities.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Monoist Sentry
 * {B}
 * Artifact Creature — Robot
 * Defender
 */
val MonoistSentry = card("Monoist Sentry") {
    manaCost = "{B}"
    typeLine = "Artifact Creature — Robot"
    power = 4
    toughness = 1
    oracleText = "Defender"

    keywords(Keyword.DEFENDER)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "111"
        artist = "Nino Is"
        flavorText = "Monoists fight a war of attrition, knowing their victory is inevitable so long as they stay true to their faith."
        imageUri = "https://cards.scryfall.io/normal/front/a/c/acc503e2-5c3a-4200-beb0-7d193d6c869e.jpg?1752947002"
    }
}
