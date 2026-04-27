package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Sear
 * {1}{R}
 * Instant
 *
 * Sear deals 4 damage to target creature or planeswalker.
 */
val Sear = card("Sear") {
    manaCost = "{1}{R}"
    typeLine = "Instant"
    oracleText = "Sear deals 4 damage to target creature or planeswalker."

    spell {
        val t = target("target", Targets.CreatureOrPlaneswalker)
        effect = Effects.DealDamage(4, t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "154"
        artist = "Lars Grant-West"
        flavorText = "Everything becomes kindling at the right temperature."
        imageUri = "https://cards.scryfall.io/normal/front/a/e/aeb4612c-758b-4492-ba03-eb6741b4176e.jpg?1767658325"
    }
}
