package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Run Aground
 * {3}{U}
 * Instant
 * Put target artifact or creature on top of its owner's library.
 */
val RunAground = card("Run Aground") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Put target artifact or creature on top of its owner's library."

    spell {
        val victim = target(TargetFilter(GameObjectFilter.Artifact or GameObjectFilter.Creature))
        effect = Effects.Move(victim, Zone.LIBRARY, ZonePlacement.Top)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "72"
        artist = "Sara Winters"
        flavorText = "\"That's the first time I've seen the ground do the running.\"\n—Captain Brinely Rage"
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9ed573c6-2b95-4052-8b43-106cbaacb721.jpg?1783935775"
    }
}
