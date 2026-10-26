package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Bedevil
 * {B}{B}{R}
 * Instant
 * Destroy target artifact, creature, or planeswalker.
 */
val Bedevil = card("Bedevil") {
    manaCost = "{B}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Instant"
    oracleText = "Destroy target artifact, creature, or planeswalker."

    spell {
        val permanent = target(
            TargetFilter(GameObjectFilter.Artifact or GameObjectFilter.Creature or GameObjectFilter.Planeswalker)
        )
        effect = Effects.Destroy(permanent)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "157"
        artist = "Seb McKinnon"
        flavorText = "\"It's easy to get taken in by the spectacle, to enjoy a bit of naughty amusement. But make no mistake: the Cult of Rakdos is a danger.\"\n—Tajic"
        imageUri = "https://cards.scryfall.io/normal/front/8/1/81e2b96b-ecf2-4dd9-bc9d-3c46ee8c59e6.jpg?1783933657"
    }
}
