package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Mugging
 * {R}
 * Sorcery
 * Mugging deals 2 damage to target creature. That creature can't block this turn.
 */
val Mugging = card("Mugging") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Mugging deals 2 damage to target creature. That creature can't block this turn."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.DealDamage(2, creature) then Effects.CantBlock(creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "102"
        artist = "Greg Staples"
        flavorText = "Scream: muffled. Pockets: emptied. Neck function: never the same again."
        imageUri = "https://cards.scryfall.io/normal/front/0/5/05ca502f-73a3-42f3-b7ad-f69aa239900a.jpg"
    }
}
