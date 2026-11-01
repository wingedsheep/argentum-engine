package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Lava Coil
 * {1}{R}
 * Sorcery
 * Lava Coil deals 4 damage to target creature. If that creature would die this turn, exile it instead.
 */
val LavaCoil = card("Lava Coil") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Lava Coil deals 4 damage to target creature. If that creature would die this turn, exile it instead."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.DealDamage(4, creature) then Effects.MarkExileOnDeath(creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "108"
        artist = "Wesley Burt"
        flavorText = "\"Commander, we caught the Dimir spy and took her ashes into custody.\"\n—Kramm, Wojek security officer"
        imageUri = "https://cards.scryfall.io/normal/front/f/6/f6135cf4-50c2-4e44-b9b0-96e1187a2200.jpg?1783934161"
    }
}
