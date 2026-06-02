package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.dsl.Effects

/**
 * Defeat
 * {1}{B}
 * Sorcery
 * Destroy target creature with power 2 or less.
 */
val Defeat = card("Defeat") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Destroy target creature with power 2 or less."

    spell {
        val t = target("target", TargetCreature(filter = TargetFilter.Creature.powerAtMost(2)))
        effect = Effects.Move(t, Zone.GRAVEYARD, byDestruction = true)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "97"
        artist = "Dave Kendall"
        imageUri = "https://cards.scryfall.io/normal/front/6/0/60473300-0bdc-4e89-87d9-28c8d7b4d83d.jpg?1562787158"
    }
}
