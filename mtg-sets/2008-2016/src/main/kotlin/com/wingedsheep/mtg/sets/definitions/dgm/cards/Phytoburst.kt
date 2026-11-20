package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Phytoburst
 * {1}{G}
 * Sorcery
 * Target creature gets +5/+5 until end of turn.
 *
 * Canonical printing: Dragon's Maze, the card's earliest real printing.
 */
val Phytoburst = card("Phytoburst") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Target creature gets +5/+5 until end of turn."

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(5, 5, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "46"
        artist = "Izzy"
        flavorText = "Phytohydras are banned as houseplants in many residential spires. If left unfed, they find their own exit—usually at the cost of extensive property damage."
        imageUri = "https://cards.scryfall.io/normal/front/7/5/7507afc4-f504-4eb2-a86d-f99bc2860838.jpg"
    }
}
