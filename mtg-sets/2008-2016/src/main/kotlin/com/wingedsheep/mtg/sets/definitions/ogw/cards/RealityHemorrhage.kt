package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Reality Hemorrhage
 * {1}{R}
 * Instant
 * Devoid (This card has no color.)
 * Reality Hemorrhage deals 2 damage to any target.
 */
val RealityHemorrhage = card("Reality Hemorrhage") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Devoid (This card has no color.)\n" +
        "Reality Hemorrhage deals 2 damage to any target."

    keywords(Keyword.DEVOID)

    spell {
        val any = target(Targets.Any)
        effect = Effects.DealDamage(2, any)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "100"
        artist = "Chris Rallis"
        flavorText = "Tarek could not tell whether he was losing his blood or his mind."
        imageUri = "https://cards.scryfall.io/normal/front/c/0/c044168d-cb08-493d-98c1-b66b6149fe5a.jpg"
    }
}
