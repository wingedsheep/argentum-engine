package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Breathe Your Last
 * {1}{B}{B}
 * Instant
 *
 * Destroy target creature or planeswalker. You gain 1 life for each of its colors.
 *
 * Target reads are live-only, so the color count is frozen with `storeNumber` before the
 * destroy moves the permanent off the battlefield (projected colors, so layer-5 changes count).
 */
val BreatheYourLast = card("Breathe Your Last") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Destroy target creature or planeswalker. You gain 1 life for each of its colors."

    spell {
        val victim = target(Targets.CreatureOrPlaneswalker)
        effect = Effects.Pipeline {
            val colors = storeNumber(DynamicAmounts.colorCountOf(victim))
            run(Effects.Destroy(victim))
            run(Effects.GainLife(colors.amount))
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "82"
        artist = "Drew Baker"
        flavorText = "\"Bodies are so much more cooperative with the soul out of the way.\"\n—Liliana"
        imageUri = "https://cards.scryfall.io/normal/front/5/0/50eac71d-54f9-46c2-aa1d-c04c37e61f74.jpg?1783911284"
        ruling("2024-06-07", "If the target is a colorless creature or planeswalker, you won't gain any life.")
    }
}
