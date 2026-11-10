package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Spatial Contortion
 * {1}{C}
 * Instant
 * ({C} represents colorless mana.)
 * Target creature gets +3/-3 until end of turn.
 */
val SpatialContortion = card("Spatial Contortion") {
    manaCost = "{1}{C}"
    colorIdentity = ""
    typeLine = "Instant"
    oracleText = "({C} represents colorless mana.)\n" +
        "Target creature gets +3/-3 until end of turn."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(3, -3, creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "8"
        artist = "Daarken"
        flavorText = "\"For those without the Planeswalker spark, the merest touch of the Blind Eternities can kill.\"\n—Ugin"
        imageUri = "https://cards.scryfall.io/normal/front/4/e/4e2acf70-7625-4b77-83c1-0e08436da31f.jpg"
    }
}
