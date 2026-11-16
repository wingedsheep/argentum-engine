package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Ultimate Price
 * {1}{B}
 * Instant
 *
 * Destroy target monocolored creature.
 *
 * - "Monocolored" is exactly one color (CR 105.2): colorless and multicolored creatures are not
 *   legal targets. The check lives in the target filter and reads projected colors, so a creature
 *   recolored by a continuous effect is judged as it is now, and is rechecked on resolution.
 */
val UltimatePrice = card("Ultimate Price") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Destroy target monocolored creature."

    spell {
        val t = target(TargetFilter.Creature.monocolored())
        effect = Effects.Destroy(t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "82"
        artist = "Karl Kopinski"
        flavorText = "\"Let him be an example to others who would default on their debts to us.\"\n—\"The Cozen,\" Orzhov assassin"
        imageUri = "https://cards.scryfall.io/normal/front/d/2/d2b4912a-83a2-4870-8fac-81fa79da2830.jpg?1783940359"
        ruling("2024-01-12", "A monocolored creature is exactly one color. Colorless creatures aren't monocolored.")
    }
}
