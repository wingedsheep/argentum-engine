package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Finishing Blow
 * {4}{B}
 * Instant
 *
 * Destroy target creature or planeswalker.
 */
val FinishingBlow = card("Finishing Blow") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Destroy target creature or planeswalker."

    spell {
        val t = target(Targets.CreatureOrPlaneswalker)
        effect = Effects.Destroy(t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "99"
        artist = "Wylie Beckert"
        flavorText = "\"In this world, it's kill or be killed. I'd rather not be the latter.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/b/2b85a552-2119-4d9c-b7c1-c09c2d9f2f38.jpg?1783930707"
    }
}
