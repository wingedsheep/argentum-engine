package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dark Bargain
 * {3}{B}
 * Instant
 * Look at the top three cards of your library. Put two of them into your hand
 * and the other into your graveyard. Dark Bargain deals 2 damage to you.
 */
val DarkBargain = card("Dark Bargain") {
    manaCost = "{3}{B}"
    typeLine = "Instant"
    oracleText = "Look at the top three cards of your library. Put two of them into your hand and the other into your graveyard. Dark Bargain deals 2 damage to you."

    spell {
        effect = EffectPatterns.lookAtTopAndKeep(count = 3, keepCount = 2)
            .then(Effects.DealDamage(2, EffectTarget.Controller))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "83"
        artist = "Tyler Jacobson"
        flavorText = "\"I have pustules of the great Ratadrabik, very cheap. No? Surely you'll want a tincture of Nevinyrral's pulverized remains. Genuine!\""
        imageUri = "https://cards.scryfall.io/normal/front/7/1/71cbedc6-482e-42de-b310-22d3a955ad7e.jpg?1562737653"
    }
}
